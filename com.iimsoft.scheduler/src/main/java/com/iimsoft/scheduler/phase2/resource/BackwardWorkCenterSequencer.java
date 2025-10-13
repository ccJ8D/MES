package com.iimsoft.scheduler.phase2.resource;

import com.iimsoft.scheduler.common.ScheduleTask;
import com.iimsoft.scheduler.phase0.RateResolver;
import com.iimsoft.scheduler.phase1.shift.ShiftCalendarService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 倒排资源序列化（Backward Resource Sequencer）
 *
 * 目标：
 *  1. 不延后任何任务（尤其顶层/父层任务），即任务的 end 绝不超过其原始 backward 结果 end
 *  2. 若同一资源上存在时间重叠，只允许“更早的”任务整体向前（更早的时间）挪动消除冲突
 *  3. 保持父子约束：子任务必须在所有父任务 start 之前完成（child.end <= min(parent.start)）
 *  4. 可选：限制任务不被提前到 originalBackwardStart 之前（配置 keepNotEarlierThanOriginalBackward = true）
 *
 * 依赖语义（与现有顺排版本一致）：
 *  - ScheduleTask.predecessors 存放的是 “子任务 (child) 的 taskId 集合”
 *  - 父任务要开工，需要这些子任务全部完成
 *  => 因此：child -> parents 映射需要我们自己构建（扫描一次）
 *
 * 使用步骤（与 forward 版类似）：
 *  BackwardWorkCenterSequencer bw = new BackwardWorkCenterSequencer(calendar, rateService, true);
 *  WorkCenterSequenceResult r = bw.sequence(wcId, allTasks);
 */
public class BackwardWorkCenterSequencer {

    private final ShiftCalendarService calendar;
    private final RateResolver rateResolver;
    private final boolean keepNotEarlierThanOriginalBackward;

    public BackwardWorkCenterSequencer(ShiftCalendarService calendar,
                                       RateResolver rateResolver,
                                       boolean keepNotEarlierThanOriginalBackward) {
        this.calendar = calendar;
        this.rateResolver = rateResolver;
        this.keepNotEarlierThanOriginalBackward = keepNotEarlierThanOriginalBackward;
    }

    /**
     * 对单个工作中心执行倒排资源序列化。
     * @param workCenterId 资源ID
     * @param allTasks 全部任务（包含其它资源的，内部会过滤）
     */
    public WorkCenterSequenceResult sequence(int workCenterId,
                                             List<ScheduleTask> allTasks) {

        // 1. 过滤出该资源任务
        List<ScheduleTask> wcTasks = new ArrayList<>();
        for (ScheduleTask t : allTasks) {
            if (t.getWorkCenterId() == workCenterId) {
                wcTasks.add(t);
            }
        }
        if (wcTasks.isEmpty()) {
            return new WorkCenterSequenceResult(workCenterId, wcTasks, 0, 0);
        }

        // 2. 构建 childId -> parents 映射（由于 predecessors = 子任务IDs）
        Map<Integer, List<ScheduleTask>> childToParents = buildChildToParentsIndex(allTasks);

        // 3. 按原 backward end 降序排序（最晚结束的先“锁定”）
        wcTasks.sort((a, b) -> {
            int c = b.getEnd().compareTo(a.getEnd());
            if (c != 0) return c;
            return Integer.compare(a.getTaskId(), b.getTaskId());
        });

        int moved = 0;
        // 已被“锁定”的（更晚的）任务中最早的 start，用于限制前面任务的 latest finish
        LocalDateTime earliestLockedStart = null;

        for (ScheduleTask task : wcTasks) {
            LocalDateTime originalStart = task.getStart();
            LocalDateTime originalEnd = task.getEnd();
            if (originalStart == null || originalEnd == null) {
                // 防御：如果有空时间，跳过或抛异常
                continue;
            }

            // 4. 计算该任务的最晚允许完成时间界限 latestFinishBound
            LocalDateTime latestFinishBound = originalEnd;

            // 4.1 不得晚于已经锁定的更晚任务的最早 start（防止重叠 / 覆盖）
            if (earliestLockedStart != null && latestFinishBound.isAfter(earliestLockedStart)) {
                latestFinishBound = earliestLockedStart;
            }

            // 4.2 父任务约束：child.end <= min(parent.start)
            // 根据映射找到所有 parents
            List<ScheduleTask> parents = childToParents.get(task.getTaskId());
            if (parents != null && !parents.isEmpty()) {
                for (ScheduleTask p : parents) {
                    if (p.getStart() != null && p.getStart().isBefore(latestFinishBound)) {
                        latestFinishBound = p.getStart();
                    }
                }
            }

            // 4.3 根据工时从 latestFinishBound 反推 start
            int hoursInt = task.getProcessHours().setScale(0, BigDecimal.ROUND_UP).intValue();
            LocalDateTime newStart = calendar.subtractWholeHours(workCenterId, latestFinishBound, hoursInt);
            LocalDateTime newEnd = calendar.addWholeHours(workCenterId, newStart, hoursInt);
            // 保险：newEnd 不应超过 latestFinishBound；若超过（因班次对齐差异），截到 latestFinishBound
            if (newEnd.isAfter(latestFinishBound)) {
                // 尝试向前再缩一小时直到满足（极端防御，正常不触发）
                while (newEnd.isAfter(latestFinishBound) && hoursInt > 0) {
                    hoursInt--;
                    newStart = calendar.subtractWholeHours(workCenterId, latestFinishBound, hoursInt);
                    newEnd = calendar.addWholeHours(workCenterId, newStart, hoursInt);
                }
            }

            // 4.4 限制不能早于 originalBackwardStart（如果需要保守）
            if (keepNotEarlierThanOriginalBackward
                    && task.getOriginalBackwardStart() != null
                    && newStart.isBefore(task.getOriginalBackwardStart())) {
                // 放弃提前（保持原窗口）
                newStart = originalStart;
                newEnd = originalEnd;
            }

            // 4.5 若确实发生变化并且是“向前移动”（即 newEnd <= originalEnd）
            boolean changed = (!newStart.equals(originalStart) || !newEnd.equals(originalEnd));
            if (changed) {
                // 仅当没有延后（保障：newEnd 不晚于 originalEnd）
                if (newEnd.isAfter(originalEnd)) {
                    // 若出现延后，放弃（不应发生）
                } else {
                    task.setStart(newStart);
                    task.setEnd(newEnd);
                    moved++;
                }
            }

            // 4.6 更新 earliestLockedStart
            if (earliestLockedStart == null || task.getStart().isBefore(earliestLockedStart)) {
                earliestLockedStart = task.getStart();
            }
        }

        // 5. 为查看方便可按 start 升序再返回（不修改逻辑需求）
        wcTasks.sort(Comparator.comparing(ScheduleTask::getStart)
                .thenComparingInt(ScheduleTask::getTaskId));

        return new WorkCenterSequenceResult(workCenterId, wcTasks, moved, wcTasks.size());
    }

    /**
     * 构建 childId -> parents 映射。
     * （因为 ScheduleTask.predecessors 保存的是“子任务ID”，需反向来找父）
     */
    private Map<Integer, List<ScheduleTask>> buildChildToParentsIndex(List<ScheduleTask> all) {
        Map<Integer, ScheduleTask> idMap = new HashMap<>();
        for (ScheduleTask t : all) {
            idMap.put(t.getTaskId(), t);
        }
        Map<Integer, List<ScheduleTask>> index = new HashMap<>();
        for (ScheduleTask parent : all) {
            if (parent.getPredecessors() == null) continue;
            for (Integer childId : parent.getPredecessors()) {
                ScheduleTask child = idMap.get(childId);
                if (child == null) continue;
                index.computeIfAbsent(childId, k -> new ArrayList<>()).add(parent);
            }
        }
        return index;
    }
}