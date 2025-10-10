package com.iimsoft.scheduler.v5;

// 1. 主程序入口 - MainApp.java
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.time.LocalDate;
import java.util.*;
import com.iimsoft.scheduler.v5.db.*;
import com.iimsoft.scheduler.v5.scheduling.*;
import com.iimsoft.scheduler.v5.model.*;
import com.iimsoft.scheduler.v5.optimization.*;

public class MainApp {
    public static void main(String[] args) {
        try {
            // === 1. 数据库连接配置 ===
            String url = "jdbc:postgresql://localhost:5432/mesdb";
            String user = "postgres";
            String password = "password";
            
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName("org.postgresql.Driver");
            ds.setUrl(url);
            ds.setUsername(user);
            ds.setPassword(password);
            
            JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
            
            // === 2. 初始化各组件 ===
            DatabaseLoader loader = new DatabaseLoader(jdbcTemplate);
            ResourceCalendar calendar = new ResourceCalendar();
            calendar.initShifts(loader.loadProductionShifts());
            
            // === 3. TOC瓶颈分析（按 router 识别瓶颈工艺路线） ===
            BottleneckAnalyzer analyzer = new BottleneckAnalyzer(jdbcTemplate, calendar);
            String bottleneckRouter = analyzer.identifyBottleneck(
                LocalDate.now(), LocalDate.now().plusDays(3));
            System.out.println("识别到瓶颈工艺路线: " + bottleneckRouter);
            
            // === 4. 加载待排程工单 ===
            List<ShopOrder> orders = loader.loadPendingShopOrders();
            System.out.println("加载到 " + orders.size() + " 个待排程工单");
            
            // === 5. 初始化排程引擎和目标计算器 ===
            SchedulingEngine engine = new SchedulingEngine(calendar, loader);
            ObjectiveCalculator calculator = new ObjectiveCalculator(
                engine, bottleneckRouter, jdbcTemplate);
            
            // === 6. NSGA-II多目标优化 ===
            NSGA2Optimizer nsga2 = new NSGA2Optimizer(
                calculator, orders, 30, 50, 0.9, 0.1);
            
            System.out.println("开始NSGA-II优化...");
            List<Chromosome> paretoFront = nsga2.optimize();
            System.out.println("优化完成，获得 " + paretoFront.size() + " 个Pareto解");
            
            // === 7. 保存优化结果 ===
            ScheduleRepository scheduleRepo = new ScheduleRepository(jdbcTemplate);
            InventoryCurveRepository invRepo = new InventoryCurveRepository(jdbcTemplate);
            
            long solutionIdCounter = 1000L;
            int index = 1;
            
            for (Chromosome solution : paretoFront) {
                long solutionId = solutionIdCounter++;
                
                // 保存排程结果到MOM_SHOP_ORDER_SCHEDULE
                for (ShopOrder order : solution.getSequence()) {
                    ScheduleResult result = engine.scheduleOrder(order, bottleneckRouter);
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