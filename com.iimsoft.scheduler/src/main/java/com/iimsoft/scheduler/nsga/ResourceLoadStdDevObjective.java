package com.iimsoft.scheduler.nsga;

import com.iimsoft.scheduler.model.ScheduleTask;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 目标：最小化资源负载标准差（期望均衡）。
 * 负载 = ΣprocessHours / (spanHours(资源最早到最晚任务))
 */
public class ResourceLoadStdDevObjective implements ScheduleObjective {

    public double evaluate(List<ScheduleTask> tasks) {
        Map<Integer, List<ScheduleTask>> byWC = new HashMap<Integer, List<ScheduleTask>>();
        for (ScheduleTask t : tasks) {
            byWC.computeIfAbsent(t.getWorkCenterId(), k -> new ArrayList<ScheduleTask>()).add(t);
        }
        if (byWC.isEmpty()) return 0d;

        List<Double> loads = new ArrayList<Double>();
        for (Map.Entry<Integer, List<ScheduleTask>> e : byWC.entrySet()) {
            LocalDateTime min = null, max = null;
            double sumProc = 0d;
            for (ScheduleTask t : e.getValue()) {
                sumProc += t.getProcessHours().doubleValue();
                if (min == null || t.getStart().isBefore(min)) min = t.getStart();
                if (max == null || t.getEnd().isAfter(max)) max = t.getEnd();
            }
            long span = Duration.between(min, max).toHours();
            double load = span <= 0 ? 0d : sumProc / span;
            loads.add(load);
        }
        double mean = 0d;
        for (double v : loads) mean += v;
        mean /= loads.size();
        double var = 0d;
        for (double v : loads) {
            double d = v - mean;
            var += d * d;
        }
        var /= loads.size();
        return Math.sqrt(var);
    }

    public String name() { return "loadStdDev"; }
}