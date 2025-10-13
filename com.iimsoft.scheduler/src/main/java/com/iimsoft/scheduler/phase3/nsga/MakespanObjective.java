package com.iimsoft.scheduler.phase3.nsga;

import com.iimsoft.scheduler.common.ScheduleTask;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 目标：最小化 Makespan (小时)
 */
public class MakespanObjective implements ScheduleObjective {

    public double evaluate(List<ScheduleTask> tasks) {
        LocalDateTime min = null, max = null;
        for (ScheduleTask t : tasks) {
            if (min == null || t.getStart().isBefore(min)) min = t.getStart();
            if (max == null || t.getEnd().isAfter(max)) max = t.getEnd();
        }
        if (min == null || max == null) return 0d;
        long h = Duration.between(min, max).toHours();
        return (double) h;
    }

    public String name() { return "makespan"; }
}