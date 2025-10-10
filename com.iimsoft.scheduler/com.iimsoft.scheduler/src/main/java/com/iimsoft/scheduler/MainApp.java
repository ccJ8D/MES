package com.iimsoft.scheduler;

// 1. 主程序入口 - MainApp.java
import java.time.LocalDate;
import java.util.*;

import com.iimsoft.scheduler.db.DatabaseLoader;
import com.iimsoft.scheduler.db.InventoryCurveRepository;
import com.iimsoft.scheduler.db.ScheduleRepository;
import com.iimsoft.scheduler.model.Chromosome;
import com.iimsoft.scheduler.model.ScheduleResult;
import com.iimsoft.scheduler.model.ShopOrder;
import com.iimsoft.scheduler.optimization.NSGA2Optimizer;
import com.iimsoft.scheduler.optimization.ObjectiveCalculator;
import com.iimsoft.scheduler.scheduling.BottleneckAnalyzer;
import com.iimsoft.scheduler.scheduling.ResourceCalendar;
import com.iimsoft.scheduler.scheduling.SchedulingEngine;

public class MainApp {
    public static void main(String[] args) {
        try {
            // === 1. 数据库连接配置 ===
            String url = "jdbc:postgresql://localhost:5432/mesdb";
            String user = "postgres";
            String password = "password";
            

            
            // === 2. 初始化各组件 ===
            DatabaseLoader loader = new DatabaseLoader();
            ResourceCalendar calendar = new ResourceCalendar();
            calendar.initShifts(loader.loadProductionShifts());
            
            // === 3. TOC瓶颈分析 ===
            BottleneckAnalyzer analyzer = new BottleneckAnalyzer(calendar);
            String bottleneckId = analyzer.identifyBottleneck(
                LocalDate.now(), LocalDate.now().plusDays(3));

            
            // === 4. 加载待排程工单 ===
            List<ShopOrder> orders = loader.loadPendingShopOrders();

            
            // === 5. 初始化排程引擎和目标计算器 ===
            SchedulingEngine engine = new SchedulingEngine(calendar, loader);
            ObjectiveCalculator calculator = new ObjectiveCalculator(
                engine, bottleneckId);
            
            // === 6. NSGA-II多目标优化 ===
            NSGA2Optimizer nsga2 = new NSGA2Optimizer(
                calculator, orders, 30, 50, 0.9, 0.1);
            

            List<Chromosome> paretoFront = nsga2.optimize();

            
            // === 7. 保存优化结果 ===
            ScheduleRepository scheduleRepo = new ScheduleRepository();
            InventoryCurveRepository invRepo = new InventoryCurveRepository();
            
            long solutionIdCounter = 1000L;
            int index = 1;
            
            for (Chromosome solution : paretoFront) {
                long solutionId = solutionIdCounter++;
                
                // 保存排程结果到MOM_SHOP_ORDER_SCHEDULE
                for (ShopOrder order : solution.getSequence()) {
                    ScheduleResult result = engine.scheduleOrder(order, bottleneckId);
                    scheduleRepo.saveSchedule(result);
                }
                
                // 保存库存曲线
                calculator.evaluate(solution); // 重新评估以获取库存曲线
                List<ObjectiveCalculator.InventoryPoint> curve = calculator.getInventoryCurve();
                invRepo.saveCurve(solutionId, curve);
                
                System.out.println("保存Pareto解 #" + index + " (ID: " + solutionId + 
                    ") 目标值: " + Arrays.toString(solution.getObjectives()));
                index++;
            }
            
            System.out.println("=== 优化完成 ===");
            System.out.println("排程结果已保存到 MOM_SHOP_ORDER_SCHEDULE");
            System.out.println("库存曲线已保存到 MOM_SCHEDULE_INVENTORY_CURVE");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}