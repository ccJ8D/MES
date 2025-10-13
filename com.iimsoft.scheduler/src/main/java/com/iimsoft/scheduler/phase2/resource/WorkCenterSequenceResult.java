package com.iimsoft.scheduler.phase2.resource;

import com.iimsoft.scheduler.common.ScheduleTask;

import java.util.List;

public class WorkCenterSequenceResult {
    private final int workCenterId;
    private final List<ScheduleTask> orderedTasks;
    private final int movedCount;
    private final int total;

    public WorkCenterSequenceResult(int workCenterId,
                                    List<ScheduleTask> orderedTasks,
                                    int movedCount,
                                    int total) {
        this.workCenterId = workCenterId;
        this.orderedTasks = orderedTasks;
        this.movedCount = movedCount;
        this.total = total;
    }

    public int getWorkCenterId() { return workCenterId; }
    public List<ScheduleTask> getOrderedTasks() { return orderedTasks; }
    public int getMovedCount() { return movedCount; }
    public int getTotal() { return total; }
}