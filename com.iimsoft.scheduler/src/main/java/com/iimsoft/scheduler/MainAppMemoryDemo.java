package com.iimsoft.scheduler;

import com.iimsoft.scheduler.common.ScheduleTask;
import com.iimsoft.scheduler.phase0.BootstrapContext;
import com.iimsoft.scheduler.phase0.memory.MemoryDataBuilder;
import com.iimsoft.scheduler.phase1.Phase1Facade;
import com.iimsoft.scheduler.phase2.Phase2Facade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;


public class MainAppMemoryDemo {

    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        LocalDate d1 = today.plusDays(1);
        LocalDate d2 = today.plusDays(2);

// 物料ID
        int TOP1 = 10001, TOP2 = 10002;
        int SUB1 = 20001, SUB2 = 20002, SUB3 = 20003; // SUB3为两个顶层的公共件

// 产线ID
        int WC1 = 11, WC2 = 12, WC3 = 13, WC4 = 14, WC5 = 15;

// 构建模拟数据
        MemoryDataBuilder builder = new MemoryDataBuilder()

                // 顶层日需求
                .addDemand(TOP1, d1, 100)
                .addDemand(TOP1, d2, 120)
                .addDemand(TOP2, d1, 80)
                .addDemand(TOP2, d2, 90)

                // BOM结构（每个顶层2子件，且有1个通用件）
                .addBom(TOP1, SUB1, 2)    // 顶层1->子1
                .addBom(TOP1, SUB3, 1)    // 顶层1->通用件
                .addBom(TOP2, SUB2, 3)    // 顶层2->子2
                .addBom(TOP2, SUB3, 2)    // 顶层2->通用件

                // 分配工作中心
                .mapItemToWC(TOP1, WC1)
                .mapItemToWC(TOP2, WC2)
                .mapItemToWC(SUB1, WC3)
                .mapItemToWC(SUB2, WC4)
                .mapItemToWC(SUB3, WC5)

                // 每条产线的不同班次（可自由组合）
                .addShift(WC1, LocalTime.of(8,0), LocalTime.of(18,0))
                .addShift(WC2, LocalTime.of(8,0), LocalTime.of(18,0))
                .addShift(WC3, LocalTime.of(8,0), LocalTime.of(18,0))
                .addShift(WC4, LocalTime.of(8,0), LocalTime.of(18,30))
                .addShift(WC5, LocalTime.of(22,0), LocalTime.of(6,0)) // 夜班跨天

                // 可选：物料级产率
                .itemRate(TOP1, new BigDecimal(1))
                .itemRate(TOP2, new BigDecimal(1))
                .itemRate(SUB1, new BigDecimal(1))
                .itemRate(SUB2, new BigDecimal(1))
                .itemRate(SUB3, new BigDecimal(1));
        BootstrapContext build = builder.build();




        Phase1Facade phase1 = new Phase1Facade(build.getCalendar(), build.getRateResolver(),build.getBomProvider(),10,BigDecimal.valueOf(1000),build.getWorkCenterResolver());
        Phase1Facade.Result result1 = phase1.taskBuilding(build.getTopDemands());

        Phase2Facade phase2 = new Phase2Facade(build.getCalendar(), build.getRateResolver(),true,true,true);
        Phase2Facade.Result result2 = phase2.sequence(result1.getAllTasks());
        // === 输出 Phase1 结果（顶层任务、所有子任务、所有任务） ===


        System.out.println("\n==== Phase2 所有任务 ====");
        System.out.printf("%-6s %-6s %-6s %-8s %-8s %-16s %-16s %-10s%n",
                "TID", "Item", "WC", "Qty", "Hours", "Start", "End", "Children");
        for (ScheduleTask t : result2.getAllTasks()) {
            System.out.printf("%-6d %-6d %-6d %-8s %-8s %-16s %-16s %-10s%n",
                    t.getTaskId(), t.getItemId(), t.getWorkCenterId(), t.getQuantity(), t.getProcessHours(),
                    t.getStart(), t.getEnd(), t.getPredecessors());
        }

    }
}