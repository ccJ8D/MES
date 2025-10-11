package com.iimsoft.scheduler.nsga;

import com.iimsoft.scheduler.model.ScheduleTask;
import com.iimsoft.scheduler.util.DependencyIndex;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 目标：最小化总 Slack（子件到最早父 start 的间隔小时）
 */
public class TotalSlackObjective implements ScheduleObjective {

    public double evaluate(List<ScheduleTask> tasks) {
        DependencyIndex dep = new DependencyIndex(tasks);
        double sum = 0d;
        for (ScheduleTask child : tasks) {
            Set<ScheduleTask> parents = dep.getParentsOf(child.getTaskId());
            if (parents.isEmpty()) continue;
            LocalDateTime minParentStart = null;
            for (ScheduleTask p : parents) {
                if (minParentStart == null || p.getStart().isBefore(minParentStart)) {
                    minParentStart = p.getStart();
                }
            }
            if (minParentStart != null && !minParentStart.isBefore(child.getEnd())) {
                long m = Duration.between(child.getEnd(), minParentStart).toMinutes();
                sum += (m / 60.0);
            }
        }
        return sum;
    }

    public String name() { return "totalSlack"; }
}