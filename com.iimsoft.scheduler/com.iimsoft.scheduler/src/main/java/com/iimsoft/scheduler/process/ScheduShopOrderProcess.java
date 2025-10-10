package com.iimsoft.scheduler.process;

import com.iimsoft.process.JavaProcess;
import com.iimsoft.scheduler.db.DatabaseLoader;
import com.iimsoft.scheduler.model.Chromosome;
import com.iimsoft.scheduler.model.ShopOrder;
import com.iimsoft.scheduler.optimization.NSGA2Optimizer;
import com.iimsoft.scheduler.optimization.ObjectiveCalculator;
import com.iimsoft.scheduler.scheduling.BottleneckAnalyzer;
import com.iimsoft.scheduler.scheduling.ResourceCalendar;
import com.iimsoft.scheduler.scheduling.SchedulingEngine;

import java.time.LocalDate;
import java.util.List;

public class ScheduShopOrderProcess extends JavaProcess {
    @Override
    public String doIt() throws Exception {
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

        return "";
    }
}
