package com.iimsoft.scheduler.tigent;

import com.iimsoft.scheduler.model.ScheduleTask;

import java.time.LocalDateTime;

/**
 * 用于回滚/报告的快照
 */
public class TaskSnapshot {
    public final int taskId;
    public final LocalDateTime start;
    public final LocalDateTime end;

    public TaskSnapshot(ScheduleTask t) {
        this.taskId = t.getTaskId();
        this.start = t.getStart();
        this.end = t.getEnd();
    }
}