package com.iimsoft.scheduler.phase2;

import com.iimsoft.scheduler.common.ScheduleTask;
import com.iimsoft.scheduler.facade.Phase4ResourceSequencingProcessor;
import com.iimsoft.scheduler.phase1.service.RateService;
import com.iimsoft.scheduler.phase1.service.ShiftCalendarService;
import com.iimsoft.scheduler.phase2.resource.BackwardWorkCenterSequencer;
import com.iimsoft.scheduler.phase2.resource.WorkCenterSequenceResult;
import com.iimsoft.scheduler.phase2.resource.WorkCenterSequencer;
import com.iimsoft.scheduler.util.DependencyIndex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Phase2Facade {
    private final ShiftCalendarService calendar;
    private final RateService rateService;
    private final boolean allowEqualEndStart;
    private final boolean keepBackwardJIT;
    private final boolean strictPredecessorFinish;

    public Phase2Facade(ShiftCalendarService calendar, RateService rateService, boolean allowEqualEndStart, boolean keepBackwardJIT, boolean strictPredecessorFinish) {
        this.calendar = calendar;
        this.rateService = rateService;
        this.allowEqualEndStart = allowEqualEndStart;
        this.keepBackwardJIT = keepBackwardJIT;
        this.strictPredecessorFinish = strictPredecessorFinish;
    }

    public Result sequence(List<ScheduleTask> tasks) {
        // 1. 按工作中心分组
        Map<Integer, List<ScheduleTask>> byWC = new HashMap<Integer, List<ScheduleTask>>();
        for (ScheduleTask t : tasks) {
            List<ScheduleTask> list = byWC.get(t.getWorkCenterId());
            if (list == null) {
                list = new ArrayList<ScheduleTask>();
                byWC.put(t.getWorkCenterId(), list);
            }
            list.add(t);
        }

        List<WorkCenterSequenceResult> perWC = new ArrayList<WorkCenterSequenceResult>();
        WorkCenterSequencer sequencer = new WorkCenterSequencer(
                calendar,
                rateService,
                allowEqualEndStart,
                keepBackwardJIT,
                strictPredecessorFinish
        );

        BackwardWorkCenterSequencer backwardSequencer = new BackwardWorkCenterSequencer(
                calendar,
                rateService,
                allowEqualEndStart
        );
        for (Map.Entry<Integer, List<ScheduleTask>> e : byWC.entrySet()) {
            perWC.add(backwardSequencer.sequence(e.getKey(), tasks));
        }

        return new Result(tasks, perWC);
    }

    public static class Result {
        private final List<ScheduleTask> allTasks;
        private final List<WorkCenterSequenceResult> perWorkCenter;

        public Result(List<ScheduleTask> allTasks,
                      List<WorkCenterSequenceResult> perWorkCenter) {
            this.allTasks = allTasks;
            this.perWorkCenter = perWorkCenter;
        }

        public List<ScheduleTask> getAllTasks() { return allTasks; }
        public List<WorkCenterSequenceResult> getPerWorkCenter() { return perWorkCenter; }

        public String summary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Resource Sequencing Summary:\n");
            for (WorkCenterSequenceResult r : perWorkCenter) {
                sb.append("  WC ").append(r.getWorkCenterId())
                        .append(" total=").append(r.getTotal())
                        .append(" moved=").append(r.getMovedCount()).append("\n");
            }
            return sb.toString();
        }
    }
}
