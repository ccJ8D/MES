package com.iimsoft.scheduler.phase2.resource;

import com.iimsoft.scheduler.common.ScheduleTask;
import com.iimsoft.scheduler.phase1.shift.ShiftCalendarService;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 简单资源时间线：
 *  - 顺序放置任务（假设外部已拓扑排序）
 *  - 不做间隙最优填充，只做“最早可开工”前推
 *  - 可配置是否允许 end == next start
 */
public class ResourceTimeline {

    private final ShiftCalendarService calendar;
    private final int workCenterId;
    private final boolean allowEqualEndStart;

    private LocalDateTime cursor; // 当前资源已排到的末尾(右边界)
    private final List<ScheduleTask> placed = new ArrayList<ScheduleTask>();

    public ResourceTimeline(ShiftCalendarService calendar,
                            int workCenterId,
                            boolean allowEqualEndStart) {
        this.calendar = calendar;
        this.workCenterId = workCenterId;
        this.allowEqualEndStart = allowEqualEndStart;
    }

    public void place(ScheduleTask task,
                      LocalDateTime earliestCandidate,
                      int processHoursInt) {

        // 1. earliestCandidate align 到可工作整点
        LocalDateTime aligned = calendar.addWholeHours(workCenterId, earliestCandidate, 0); // 0 表示仅对齐（利用 addWholeHours 的 align）
        // 2. 确保不与已有任务冲突
        if (cursor != null) {
            if (allowEqualEndStart) {
                if (aligned.isBefore(cursor)) {
                    aligned = cursor;
                }
            } else {
                if (!aligned.isAfter(cursor)) { // 需要严格 >
                    aligned = cursor;
                }
            }
        }
        // 3. 计算 end
        LocalDateTime end = calendar.addWholeHours(workCenterId, aligned, processHoursInt);

        // 4. 回写
        task.setStart(aligned);
        task.setEnd(end);

        // 5. 更新 cursor
        cursor = end;
        placed.add(task);
    }

    public List<ScheduleTask> getPlaced() {
        return placed;
    }
}