package com.iimsoft.scheduler;


import com.iimsoft.scheduler.bom.BomProvider;
import com.iimsoft.scheduler.bom.InMemoryBomProvider;
import com.iimsoft.scheduler.facade.*;
import com.iimsoft.scheduler.kpi.KpiCollector;
import com.iimsoft.scheduler.merge.BatchMergeStrategy;
import com.iimsoft.scheduler.merge.BatchMerger;
import com.iimsoft.scheduler.merge.SimpleExactWindowMergeStrategy;
import com.iimsoft.scheduler.model.DailyDemand;
import com.iimsoft.scheduler.model.ScheduleTask;
import com.iimsoft.scheduler.nsga.Chromosome;
import com.iimsoft.scheduler.service.RateService;
import com.iimsoft.scheduler.shift.WorkCalendarService;
import com.iimsoft.scheduler.tigent.TighteningConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Phase1 演示入口：
 *  - 构造下个月前 5 天的顶层需求
 *  - 注册班次
 *  - 产能：50 件/小时
 *  - Backward 计算 start/end
 */
public class MainApp {

    public static void main(String[] args) {
        LocalDate nm = LocalDate.now().withDayOfMonth(1).plusMonths(1);
        List<DailyDemand> demands = new ArrayList<DailyDemand>();
        demands.add(new DailyDemand(10001, nm.plusDays(0), new BigDecimal("160"))); // 160 -> 3.2h -> 4h (ceil)
        demands.add(new DailyDemand(10001, nm.plusDays(1), new BigDecimal("200")));

        // 日历
        WorkCalendarService calendar = new WorkCalendarService();
        calendar.registerDailyTemplate(1, Arrays.asList(
                new WorkCalendarService.DailyTemplate(LocalTime.of(8,0), LocalTime.of(12,0)),
                new WorkCalendarService.DailyTemplate(LocalTime.of(13,0), LocalTime.of(17,0))
        ));
        calendar.registerDailyTemplate(0, Arrays.asList(
                new WorkCalendarService.DailyTemplate(LocalTime.of(8,0), LocalTime.of(12,0)),
                new WorkCalendarService.DailyTemplate(LocalTime.of(13,0), LocalTime.of(17,0))
        ));

        RateService rateService = new RateService(new BigDecimal("50")); // 50件/小时
        BomProvider bomProvider = new InMemoryBomProvider();

        MultiLevelPhase3PlanningFacade facade = new MultiLevelPhase3PlanningFacade(
                calendar,
                rateService,
                1,                 // 顶层工作中心
                1,                 // 子件默认工作中心
                bomProvider,
                10,                // max depth
                new BigDecimal("120"), // max batch qty
                new BigDecimal("0")    // buffer hours
        );

        MultiLevelPhase3PlanningFacade.Result result = facade.plan(demands);

        Phase4ResourceSequencingProcessor processor = new Phase4ResourceSequencingProcessor(
                calendar,
                rateService,
                true,   // allowEqualEndStart
                true,   // keepBackwardJIT
                true    // strictPredecessorFinish
        );
        Phase4ResourceSequencingProcessor.Result seq = processor.process(result.getAllTasks());


//// 打印调整后的任务
//        for (ScheduleTask t : seq.getAllTasks()) {
//            System.out.printf("T%d WC=%d Start=%s End=%s%n",
//                    t.getTaskId(), t.getWorkCenterId(), t.getStart(), t.getEnd());
//        }
//

        // 1. 假设我们已有经过 sequencing 的 tasks 列表 (from previous steps)
        List<ScheduleTask> sequencedTasks = seq.getAllTasks(); // 需自建

        // 2. 执行批次合并
        BatchMerger batchMerger = new BatchMerger(rateService, true);
        BatchMergeStrategy strategy = new SimpleExactWindowMergeStrategy();
        Phase4BatchMergeProcessor processor_merge = new Phase4BatchMergeProcessor(batchMerger, strategy);

        Phase4BatchMergeProcessor.Result mergeRes = processor_merge.process(sequencedTasks);


        // C: JIT Tightening
        TighteningConfig config = TighteningConfig.defaultConfig();
        Phase4JitTighteningProcessor tighteningProcessor =
                new Phase4JitTighteningProcessor(calendar, rateService, config);
        Phase4JitTighteningProcessor.Result tightRes =
                tighteningProcessor.process(mergeRes.getTasks());



        List<ScheduleTask> tasks = tightRes.getTasks(); // 自行实现

        KpiCollector collector = new KpiCollector();
        Phase4KpiProcessor kpiprocessor = new Phase4KpiProcessor(collector);
        Phase4KpiProcessor.Result kpires = kpiprocessor.process(tasks);



        Phase5OptimizerFacade.Config cfg = new Phase5OptimizerFacade.Config();
        cfg.populationSize = 30;
        cfg.generations = 20;
        cfg.applyBatchMerge = false;
        cfg.applyTightening = false;

        Phase5OptimizerFacade optimizer = new Phase5OptimizerFacade(calendar, rateService, cfg);
        Phase5OptimizerFacade.Result res = optimizer.optimize(tasks);

        System.out.println("Final population size = " + res.finalPopulation.size());
        System.out.println("First front solutions = " + res.firstFront.size());
        int i = 0;
        for (Chromosome c : res.firstFront) {
            System.out.println("Pareto #" + (i++) + " : " + c);
        }
    }
}