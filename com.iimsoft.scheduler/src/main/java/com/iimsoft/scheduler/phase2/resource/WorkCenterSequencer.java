package com.iimsoft.scheduler.phase2.resource;

import com.iimsoft.scheduler.common.ScheduleTask;
import com.iimsoft.scheduler.phase1.service.ShiftCalendarService;
import com.iimsoft.scheduler.phase1.service.RateService;
import com.iimsoft.scheduler.util.TopoSorter;

import java.math.BigDecimal;
import java.util.*;

/**
 * 对单个工作中心进行：
 *  1) 内部拓扑排序
 *  2) Forward 放置（消除同资源重叠）
 *  3) 可配置 keepBackwardJIT：若 forward 提前，则恢复 backward start（再 forward 计算 end）
 *  4) 若 strictPredecessorFinish：如果预置放置后仍 pre.end>cur.start，继续向前推 cur.start
 */
public class WorkCenterSequencer {

    private final ShiftCalendarService calendar;
    private final RateService rateService;
    private final boolean allowEqualEndStart;
    private final boolean keepBackwardJIT;
    private final boolean strictPredecessorFinish;

    public WorkCenterSequencer(ShiftCalendarService calendar,
                               RateService rateService,
                               boolean allowEqualEndStart,
                               boolean keepBackwardJIT,
                               boolean strictPredecessorFinish) {
        this.calendar = calendar;
        this.rateService = rateService;
        this.allowEqualEndStart = allowEqualEndStart;
        this.keepBackwardJIT = keepBackwardJIT;
        this.strictPredecessorFinish = strictPredecessorFinish;
    }

    public WorkCenterSequenceResult sequence(int workCenterId,
                                             List<ScheduleTask> allTasks) {

        // 1. 过滤出该资源任务
        List<ScheduleTask> wcTasks = new ArrayList<ScheduleTask>();
        for (ScheduleTask t : allTasks) {
            if (t.getWorkCenterId() == workCenterId) {
                wcTasks.add(t);
            }
        }
        if (wcTasks.isEmpty()) {
            return new WorkCenterSequenceResult(workCenterId, wcTasks, 0, 0);
        }

        // 2. 本资源任务ID集合
        Set<Integer> wcIds = new HashSet<Integer>();
        for (ScheduleTask t : wcTasks) wcIds.add(t.getTaskId());

        // 3. 拓扑排序（内部依赖）
        List<ScheduleTask> topo = TopoSorter.topoOrderForWorkCenter(wcTasks, wcIds);

        // 4. 建立全局 id -> task
        Map<Integer, ScheduleTask> idMap = new HashMap<Integer, ScheduleTask>();
        for (ScheduleTask t : allTasks) idMap.put(t.getTaskId(), t);

        ResourceTimeline timeline = new ResourceTimeline(calendar, workCenterId, allowEqualEndStart);

        int moved = 0;

        for (ScheduleTask task : topo) {
            // 4.1 计算 earliestCandidate = max(所有前置 end)
            java.time.LocalDateTime earliest = task.getStart(); // backward 结果
            for (Integer pre : task.getPredecessors()) {
                ScheduleTask p = idMap.get(pre);
                if (p == null) continue;
                if (p.getEnd() != null && p.getEnd().isAfter(earliest)) {
                    earliest = p.getEnd();
                }
            }

            // 4.2 keepBackwardJIT：不让任务早于 backward start
            java.time.LocalDateTime originalBackwardStart = task.getStart();
            java.time.LocalDateTime candidate = earliest;
            if (keepBackwardJIT && candidate.isBefore(originalBackwardStart)) {
                candidate = originalBackwardStart;
            }

            // 4.3 strict predecessor finish（如果前置结束时间 > candidate，再对齐）
            if (strictPredecessorFinish) {
                java.time.LocalDateTime maxPreEnd = null;
                for (Integer pid : task.getPredecessors()) {
                    ScheduleTask p = idMap.get(pid);
                    if (p == null) continue;
                    if (maxPreEnd == null || p.getEnd().isAfter(maxPreEnd)) {
                        maxPreEnd = p.getEnd();
                    }
                }
                if (maxPreEnd != null && maxPreEnd.isAfter(candidate)) {
                    candidate = maxPreEnd;
                }
            }

            // 4.4 按资源时间线放置
            int hoursInt = task.getProcessHours().setScale(0, BigDecimal.ROUND_UP).intValue();
            java.time.LocalDateTime oldStart = task.getStart();
            timeline.place(task, candidate, hoursInt);

            if (!task.getStart().equals(oldStart)) moved++;
        }

        return new WorkCenterSequenceResult(workCenterId, timeline.getPlaced(), moved, wcTasks.size());
    }
}