package com.iimsoft.scheduler.optimization;

// 14. 目标计算器 - optimization/ObjectiveCalculator.java

import com.iimsoft.scheduler.model.Chromosome;
import com.iimsoft.scheduler.model.ShopOrder;
import com.iimsoft.scheduler.scheduling.SchedulingEngine;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class ObjectiveCalculator {
    private final SchedulingEngine engine;
    private final String bottleneckResource;

    private final List<InventoryPoint> inventoryCurve = new ArrayList<>();

    public ObjectiveCalculator(SchedulingEngine engine, String bottleneckResource
    ) {
        this.engine = engine;
        this.bottleneckResource = bottleneckResource;
    }

    public double[] evaluate(Chromosome chromosome) {
        inventoryCurve.clear();
        List<ShopOrder> sequence = chromosome.getSequence();

        double makespan = 0;
        double totalLateness = 0;
        List<Double> inventoryLevels = new ArrayList<>();

        // 1. 初始库存
        double currentInventory = loadInitialInventory();
        inventoryLevels.add(currentInventory);
        inventoryCurve.add(new InventoryPoint(new Date(), currentInventory, "初始库存"));

        // 2. 需求计划
        Map<String, Double> demandPlan = loadDemandPlan();

        Map<Integer, LocalDateTime> finishTimes = engine.computeCompletionTimesByDependencies(sequence);

        // 3. 模拟排程过程
        for (ShopOrder order : sequence) {
            LocalDateTime finish = finishTimes.get(order.getShopOrder());
            if (finish == null) {
                // 若意外未计算到 finish，则跳过或视为不可行（这里把其设为 now）
                finish = LocalDateTime.now();
            }

//            ScheduleResult result = engine.scheduleOrder(order, bottleneckResource);
//
//            // 计算完成时间
//            double finishEpoch = result.getStepSchedules().stream()
//                    .mapToDouble(s -> s.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
//                    .max().orElse(0);
//
//            makespan = Math.max(makespan, finishEpoch);

            double finishEpoch = finish.atZone(ZoneId.systemDefault()).toEpochSecond();
            makespan = Math.max(makespan, finishEpoch);

            // 计算延迟（若有交期）
            if (order.getPlannedCompDate() != null) {
                double dueEpoch = order.getPlannedCompDate().atZone(ZoneId.systemDefault()).toEpochSecond();
                double lateness = Math.max(0, finishEpoch - dueEpoch);
                totalLateness += lateness;
            }

            // 更新库存：生产完成时库存增加
            currentInventory += order.getQtyToBuild().doubleValue();
            Date finishTime = new Date((long) finishEpoch * 1000);
            inventoryLevels.add(currentInventory);
            inventoryCurve.add(new InventoryPoint(finishTime, currentInventory, "完成工单 " + order.getShopOrder()));

            // 发货（按 demandPlan，键可按业务调整至物料级）
            double demandQty = demandPlan.getOrDefault(order.getShopOrder(), 0.0);
            if (demandQty > 0) {
                currentInventory -= demandQty;
                inventoryLevels.add(currentInventory);
                inventoryCurve.add(new InventoryPoint(finishTime, currentInventory, "发货 " + demandQty + " 件"));
            }

//            // 计算延迟
//            double orderDueEpoch = order.getPlannedCompDate()
//                    .atZone(ZoneId.systemDefault()).toEpochSecond();
//            double lateness = Math.max(0, finishEpoch - orderDueEpoch);
//            totalLateness += lateness;
//
//            // 库存变化：生产完成
//            currentInventory += order.getQtyToBuild().doubleValue();
//            Date finishTime = new Date((long) finishEpoch * 1000);
//            inventoryLevels.add(currentInventory);
//            inventoryCurve.add(new InventoryPoint(finishTime, currentInventory,
//                    "完成工单 " + order.getShopOrder()));
//
//            // 库存变化：发货
//            double demandQty = demandPlan.getOrDefault(order.getShopOrder(), 0.0);
//            if (demandQty > 0) {
//                currentInventory -= demandQty;
//                inventoryLevels.add(currentInventory);
//                inventoryCurve.add(new InventoryPoint(finishTime, currentInventory,
//                        "发货 " + demandQty + " 件"));
//            }
        }

        // 4. 计算库存波动
        double inventoryFluctuation = calculateVariance(inventoryLevels);

        double[] objectives = {makespan, totalLateness, inventoryFluctuation};
        chromosome.setObjectives(objectives);

        return objectives;
    }

    public List<InventoryPoint> getInventoryCurve() {
        return new ArrayList<>(inventoryCurve);
    }

    private double loadInitialInventory() {
        String sql = "SELECT COALESCE(SUM(qty_on_hand), 0) FROM mom_inventory";
        Double qty = 0.0;
        return qty != null ? qty : 1000.0; // 默认1000
    }

    private Map<String, Double> loadDemandPlan() {
        String sql = "SELECT so.shop_order, SUM(d.demand_qty) as total_demand\n"
                + "FROM mom_sales_order so\n"
                + "JOIN mom_demand d ON so.sales_order_bo = d.sales_order_bo\n"
                + "GROUP BY so.shop_order";

        Map<String, Double> demandMap = new HashMap<>();

        return demandMap;
    }

    private double calculateVariance(List<Double> values) {
        if (values.isEmpty()) return 0;

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = 0;

        for (double value : values) {
            variance += Math.pow(value - mean, 2);
        }

        return variance / values.size();
    }

    public static class InventoryPoint {
        public Date timestamp;
        public double level;
        public String note;

        public InventoryPoint(Date timestamp, double level, String note) {
            this.timestamp = timestamp;
            this.level = level;
            this.note = note;
        }
    }
}