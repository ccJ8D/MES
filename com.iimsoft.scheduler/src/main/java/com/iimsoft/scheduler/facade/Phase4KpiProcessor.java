package com.iimsoft.scheduler.facade;

import com.iimsoft.scheduler.kpi.KpiCollector;
import com.iimsoft.scheduler.kpi.KpiJsonExporter;
import com.iimsoft.scheduler.kpi.KpiReport;
import com.iimsoft.scheduler.model.ScheduleTask;

import java.util.List;

public class Phase4KpiProcessor {

    private final KpiCollector collector;

    public Phase4KpiProcessor(KpiCollector collector) {
        this.collector = collector;
    }

    public Result process(List<ScheduleTask> tasks) {
        KpiReport report = collector.collect(tasks);
        return new Result(report);
    }

    public static class Result {
        private final KpiReport report;
        public Result(KpiReport report) {
            this.report = report;
        }
        public KpiReport getReport() { return report; }
        public String toJson() { return KpiJsonExporter.toJson(report); }
    }
}