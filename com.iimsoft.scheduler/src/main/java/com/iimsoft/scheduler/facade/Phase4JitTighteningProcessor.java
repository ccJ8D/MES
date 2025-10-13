package com.iimsoft.scheduler.facade;

import com.iimsoft.scheduler.common.ScheduleTask;
import com.iimsoft.scheduler.phase1.service.ShiftCalendarService;
import com.iimsoft.scheduler.phase1.service.RateService;
import com.iimsoft.scheduler.phase2.tigent.JitTightener;
import com.iimsoft.scheduler.phase2.tigent.TightenReport;
import com.iimsoft.scheduler.phase2.tigent.TighteningConfig;

import java.util.List;

public class Phase4JitTighteningProcessor {

    private final JitTightener tightener;

    public Phase4JitTighteningProcessor(ShiftCalendarService calendar,
                                        RateService rateService,
                                        TighteningConfig config) {
        this.tightener = new JitTightener(calendar, rateService, config);
    }

    public Result process(List<ScheduleTask> tasks) {
        TightenReport report = tightener.tighten(tasks);
        return new Result(tasks, report);
    }

    public static class Result {
        private final List<ScheduleTask> tasks;
        private final TightenReport report;
        public Result(List<ScheduleTask> tasks, TightenReport report) {
            this.tasks = tasks;
            this.report = report;
        }
        public List<ScheduleTask> getTasks() { return tasks; }
        public TightenReport getReport() { return report; }
    }
}