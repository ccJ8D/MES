package com.iimsoft.scheduler.facade;

import com.iimsoft.scheduler.model.ScheduleTask;

import com.iimsoft.scheduler.resource.WorkCenterSequenceResult;
import com.iimsoft.scheduler.resource.WorkCenterSequencer;
import com.iimsoft.scheduler.service.RateService;
import com.iimsoft.scheduler.shift.WorkCalendarService;

import java.util.*;

/**
 * Phase4 Step A:
 *  统一入口：对所有工作中心执行资源序列化（Forward）→ 返回调整统计。
 *  后续可在这里串联：
 *    - 批次合并
 *    - JIT Tightening
 *    - KPI 计算
 */
public class Phase4ResourceSequencingProcessor {

    private final WorkCalendarService calendar;
    private final RateService rateService;
    private final boolean allowEqualEndStart;
    private final boolean keepBackwardJIT;
    private final boolean strictPredecessorFinish;

    public Phase4ResourceSequencingProcessor(WorkCalendarService calendar,
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

    public Result process(List<ScheduleTask> tasks) {
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

        for (Map.Entry<Integer, List<ScheduleTask>> e : byWC.entrySet()) {
            perWC.add(sequencer.sequence(e.getKey(), tasks));
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