package com.iimsoft.scheduler;

// Phase1: 基础日历、产能、顶层任务
import com.iimsoft.scheduler.common.DailyDemand;
import com.iimsoft.scheduler.common.ScheduleTask;

// Phase2: BOM
import com.iimsoft.scheduler.phase1.Phase1Facade;
import com.iimsoft.scheduler.phase1.bom.BomProvider;
import com.iimsoft.scheduler.phase1.bom.InMemoryBomProvider;

// Phase3: 多层级排程

// Phase4: 资源序列化、批次合并、JIT Tightening、KPI
import com.iimsoft.scheduler.phase1.service.RateService;
import com.iimsoft.scheduler.phase1.service.ShiftCalendarService;
import com.iimsoft.scheduler.phase2.Phase2Facade;

// Phase5: NSGA-II多目标优化

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
        ShiftCalendarService calendar = new ShiftCalendarService();
        RateService rateService = new RateService(new BigDecimal("50")); // 50件/小时
        BomProvider bomProvider = new InMemoryBomProvider();

        Phase1Facade phase1 = new Phase1Facade(calendar, rateService, bomProvider,10,BigDecimal.valueOf(10000),BigDecimal.valueOf(10));
        Phase1Facade.Result result = phase1.taskBuilding(demands);
        List<ScheduleTask> tasks = result.getAllTasks();
        System.out.println("Phase1 任务数: " + tasks.size());
        System.out.println("--------------------------------------------------");
        System.out.printf("%-5s %-8s %-12s %-12s %-20s%n", "TID", "WC", "Start", "End", "Predecessors");
        System.out.println("--------------------------------------------------");
        for (ScheduleTask t : tasks) {
            System.out.printf("%-5d %-8d %-12s %-12s %-20s%n",
                    t.getTaskId(), t.getWorkCenterId(), t.getStart(), t.getEnd(), t.getPredecessors());
        }


        Phase2Facade phase2 = new Phase2Facade(calendar, rateService,true,true,true);
        Phase2Facade.Result sequence = phase2.sequence(tasks);

        System.out.println("Phase2 排序后任务数: " + sequence.getAllTasks().size());
        System.out.println("--------------------------------------------------");
        System.out.printf("%-5s %-8s %-12s %-12s %-20s%n", "TID", "WC", "Start", "End", "Predecessors");
        System.out.println("--------------------------------------------------");
        for (ScheduleTask t : sequence.getAllTasks()) {
            System.out.printf("%-5d %-8d %-12s %-12s %-20s%n",
                    t.getTaskId(), t.getWorkCenterId(), t.getStart(), t.getEnd(), t.getPredecessors());
        }



    }
}