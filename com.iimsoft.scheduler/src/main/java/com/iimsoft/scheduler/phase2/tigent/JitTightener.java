package com.iimsoft.scheduler.phase2.tigent;

import com.iimsoft.scheduler.common.ScheduleTask;
import com.iimsoft.scheduler.phase0.RateResolver;
import com.iimsoft.scheduler.phase1.shift.ShiftCalendarService;
import com.iimsoft.scheduler.util.DependencyIndex;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 多轮反向挤压：
 *  1. 构建依赖索引 child->parents
 *  2. 按工作中心分组
 *  3. 每个工作中心：拓扑（实际已有 forward 顺序），反向遍历尝试后移任务
 */
public class JitTightener {

    private final ShiftCalendarService calendar;
    private final RateResolver rateResolver;
    private final TighteningConfig config;

    public JitTightener(ShiftCalendarService calendar,
                        RateResolver rateResolver,
                        TighteningConfig config) {
        this.calendar = calendar;
        this.rateResolver = rateResolver;
        this.config = config;
    }

    public TightenReport tighten(List<ScheduleTask> tasks) {
        DependencyIndex depIndex = new DependencyIndex(tasks);

        Map<Integer, List<ScheduleTask>> byWC = new HashMap<Integer, List<ScheduleTask>>();
        for (ScheduleTask t : tasks) {
            byWC.computeIfAbsent(t.getWorkCenterId(), k -> new ArrayList<ScheduleTask>()).add(t);
        }

        Map<Integer, List<String>> messages = new LinkedHashMap<Integer, List<String>>();
        int shifted = 0;
        BigDecimal slackBefore = BigDecimal.ZERO;
        BigDecimal slackAfter = BigDecimal.ZERO;

        // 初始 slack 统计
        if (config.isComputeSlack()) {
            for (ScheduleTask t : tasks) {
                BigDecimal s = computeSlackHours(t, depIndex);
                t.setSlackHours(s);
                slackBefore = slackBefore.add(s);
            }
        }

        int iteration = 0;
        boolean anyMoved;
        do {
            anyMoved = false;
            iteration++;

            for (Map.Entry<Integer, List<ScheduleTask>> e : byWC.entrySet()) {
                List<ScheduleTask> wcTasks = e.getValue();
                // 按 start 升序已资源序列化；反向遍历
                Collections.sort(wcTasks, new Comparator<ScheduleTask>() {
                    public int compare(ScheduleTask a, ScheduleTask b) {
                        int c = a.getStart().compareTo(b.getStart());
                        if (c != 0) return c;
                        return Integer.compare(a.getTaskId(), b.getTaskId());
                    }
                });

                // 建立 resource 后继约束：同资源下下一个任务的 start
                for (int i = wcTasks.size() - 1; i >= 0; i--) {
                    ScheduleTask cur = wcTasks.get(i);
                    TaskSnapshot snap = new TaskSnapshot(cur);

                    // 计算最新可行完成界 latestFinishBound
                    LocalDateTime latestFinishBound = computeParentBound(depIndex, cur);
                    if (latestFinishBound == null) {
                        // 无父（顶层或无依赖），不强制压
                        continue;
                    }
                    // 同资源下一个任务（后一个开始时间）也可成为界（避免重叠）
                    if (i < wcTasks.size() - 1) {
                        ScheduleTask next = wcTasks.get(i + 1);
                        LocalDateTime nextStart = next.getStart();
                        if (nextStart.isBefore(latestFinishBound) || nextStart.equals(latestFinishBound)) {
                            // latestFinishBound 不应超过 nextStart（预留可配置 gap，这里简化无 gap）
                            latestFinishBound = nextStart;
                        }
                    }

                    // 如果当前 end 已经 >= latestFinishBound（或满足 strict），无需移动
                    boolean strict = config.isStrictParentBoundary();
                    int gap = config.getStrictGapHours();
                    LocalDateTime requiredFinish = strict
                            ? latestFinishBound.minusHours(gap)  // child.end <= parent.start - gap
                            : latestFinishBound;                // child.end <= parent.start

                    if (cur.getEnd().isAfter(requiredFinish) || cur.getEnd().equals(requiredFinish)) {
                        continue;
                    }

                    // 重新 backward：让 end 尽量贴 requiredFinish
                    int hoursInt = cur.getProcessHours().setScale(0, RoundingMode.UP).intValue();
                    LocalDateTime newStart = calendar.subtractWholeHours(cur.getWorkCenterId(), requiredFinish, hoursInt);
                    LocalDateTime newEnd = calendar.addWholeHours(cur.getWorkCenterId(), newStart, hoursInt);

                    // 不得早于 originalBackwardStart （若配置）
                    if (config.isKeepNotEarlierThanOriginalBackward()
                            && newStart.isBefore(cur.getOriginalBackwardStart())) {
                        // 放弃移动
                        continue;
                    }

                    // 检查对其子件（predecessors=子任务）的约束：每个子任务 end <= newStart，否则冲突
                    boolean violation = false;
                    for (Integer childId : cur.getPredecessors()) {
                        ScheduleTask child = depIndex.getIdMap().get(childId);
                        if (child == null) continue;
                        if (!child.getEnd().isBefore(newStart)) {
                            violation = true;
                            break;
                        }
                    }
                    if (violation) {
                        if (config.isRollbackOnChainViolation()) {
                            continue;
                        } else {
                            // 可以尝试把 newStart 再前推到所有子 end 最大值之后
                            LocalDateTime maxChildEnd = null;
                            for (Integer childId : cur.getPredecessors()) {
                                ScheduleTask child = depIndex.getIdMap().get(childId);
                                if (child == null) continue;
                                if (maxChildEnd == null || child.getEnd().isAfter(maxChildEnd)) {
                                    maxChildEnd = child.getEnd();
                                }
                            }
                            if (maxChildEnd != null && !maxChildEnd.isBefore(newStart)) {
                                // 强制定位 newStart = maxChildEnd (+0h or +strictGap)
                                LocalDateTime forcedStart = strict ? maxChildEnd.plusHours(gap) : maxChildEnd;
                                // 保持工时
                                newStart = forcedStart;
                                newEnd = calendar.addWholeHours(cur.getWorkCenterId(), newStart, hoursInt);
                                // 再次检查不超过 parent bound：
                                if (strict && !newEnd.isBefore(latestFinishBound)) {
                                    // 放弃
                                    continue;
                                } else if (!strict && newEnd.isAfter(latestFinishBound)) {
                                    continue;
                                }
                            }
                        }
                    }

                    // 如果没有改善（未向后移动），跳过
                    if (!newStart.isAfter(cur.getStart())) {
                        continue;
                    }

                    cur.setStart(newStart);
                    cur.setEnd(newEnd);
                    anyMoved = true;
                    shifted++;

                    addMsg(messages, cur.getTaskId(),
                            "Tightened from [" + snap.start + "," + snap.end + "] to [" + newStart + "," + newEnd + "] bound=" + latestFinishBound);
                }
            }
        } while (anyMoved && iteration < config.getMaxIterations());

        if (config.isComputeSlack()) {
            for (ScheduleTask t : tasks) {
                BigDecimal s = computeSlackHours(t, depIndex);
                t.setSlackHours(s);
                slackAfter = slackAfter.add(s);
            }
        }

        return new TightenReport(tasks.size(), shifted, slackBefore, slackAfter, messages);
    }

    private void addMsg(Map<Integer, List<String>> messages, int taskId, String msg) {
        List<String> list = messages.get(taskId);
        if (list == null) {
            list = new ArrayList<String>();
            messages.put(taskId, list);
        }
        list.add(msg);
    }

    /**
     * 计算父界：所有父任务(即该任务的“parent set”= 依赖反向) 的 start 取最小；若 strict 则界为 minParentStart
     * 任务模型里：父任务含当前任务的 taskId 在其 predecessors 吗？我们构造 DependencyIndex 时 child->parents 索引：
     * 这里直接用 depIndex.getParentsOf(cur.taskId)
     */
    private LocalDateTime computeParentBound(DependencyIndex depIndex, ScheduleTask task) {
        Set<ScheduleTask> parents = depIndex.getParentsOf(task.getTaskId());
        if (parents.isEmpty()) return null;
        LocalDateTime minParentStart = null;
        for (ScheduleTask p : parents) {
            if (minParentStart == null || p.getStart().isBefore(minParentStart)) {
                minParentStart = p.getStart();
            }
        }
        return minParentStart;
    }

    private BigDecimal computeSlackHours(ScheduleTask t, DependencyIndex depIndex) {
        Set<ScheduleTask> parents = depIndex.getParentsOf(t.getTaskId());
        if (parents.isEmpty()) return BigDecimal.ZERO;
        LocalDateTime minParentStart = null;
        for (ScheduleTask p : parents) {
            if (minParentStart == null || p.getStart().isBefore(minParentStart)) {
                minParentStart = p.getStart();
            }
        }
        if (minParentStart == null) return BigDecimal.ZERO;
        long minutes = Duration.between(t.getEnd(), minParentStart).toMinutes();
        if (minutes < 0) minutes = 0;
        return new BigDecimal(minutes).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
    }
}