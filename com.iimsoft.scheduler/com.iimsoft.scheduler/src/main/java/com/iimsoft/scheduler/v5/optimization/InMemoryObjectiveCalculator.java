package com.iimsoft.scheduler.v5.optimization;

import com.iimsoft.scheduler.v5.material.model.*;
import com.iimsoft.scheduler.v5.model.*;
import com.iimsoft.scheduler.v5.scheduling.SchedulingEngine;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory implementation of objective calculation.
 * Computes makespan, total lateness, and inventory fluctuation without database access.
 * Supports per-material inventory tracking and demand fulfillment.
 */
public class InMemoryObjectiveCalculator implements ObjectiveEvaluator {
    private final SchedulingEngine engine;
    private final String bottleneckResource;
    private final MaterialPlanConfig config;
    private final Map<String, Double> initialInventory;
    private final Map<String, List<MaterialDemand>> demandsByMaterial;
    
    public InMemoryObjectiveCalculator(SchedulingEngine engine, String bottleneckResource, 
                                     MaterialPlanConfig config) {
        this.engine = engine;
        this.bottleneckResource = bottleneckResource;
        this.config = config;
        this.initialInventory = config.getInitialInventory() != null ? 
                               config.getInitialInventory() : new HashMap<>();
        
        // Group demands by material for efficient lookup
        this.demandsByMaterial = config.getDemands().stream()
            .collect(Collectors.groupingBy(MaterialDemand::getMaterialId));
    }
    
    @Override
    public double[] evaluate(Chromosome chromosome) {
        List<ShopOrder> sequence = chromosome.getSequence();
        
        double makespan = 0;
        double totalLateness = 0;
        
        // Track inventory levels for fluctuation calculation
        Map<String, Double> currentInventory = new HashMap<>(initialInventory);
        List<Double> totalInventoryLevels = new ArrayList<>();
        
        // Add initial total inventory level
        double initialTotal = currentInventory.values().stream().mapToDouble(Double::doubleValue).sum();
        totalInventoryLevels.add(initialTotal);
        
        // Simulate scheduling process
        for (ShopOrder order : sequence) {
            ScheduleResult result = engine.scheduleOrder(order, bottleneckResource);
            
            // Calculate completion time (makespan)
            double finishEpoch = result.getStepSchedules().stream()
                .mapToDouble(step -> step.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                .max()
                .orElse(0.0);
            
            makespan = Math.max(makespan, finishEpoch);
            
            // Calculate lateness
            double orderDueEpoch = order.getPlannedCompDate()
                .atZone(ZoneId.systemDefault()).toEpochSecond();
            double lateness = Math.max(0, finishEpoch - orderDueEpoch);
            totalLateness += lateness;
            
            // Update inventory: production completed
            String materialId = extractMaterialIdFromOrder(order);
            double currentMaterialInventory = currentInventory.getOrDefault(materialId, 0.0);
            currentMaterialInventory += order.getQtyToBuild();
            currentInventory.put(materialId, currentMaterialInventory);
            
            // Update total inventory level
            double currentTotal = currentInventory.values().stream().mapToDouble(Double::doubleValue).sum();
            totalInventoryLevels.add(currentTotal);
            
            // Handle demand consumption
            LocalDate orderDate = order.getPlannedCompDate().toLocalDate();
            List<MaterialDemand> materialDemands = demandsByMaterial.getOrDefault(materialId, Collections.emptyList());
            
            for (MaterialDemand demand : materialDemands) {
                if (demand.getDate().equals(orderDate)) {
                    // Consume inventory for this demand
                    double demandQty = demand.getQuantity();
                    double availableInventory = currentInventory.getOrDefault(materialId, 0.0);
                    double consumedQty = Math.min(demandQty, availableInventory);
                    
                    currentInventory.put(materialId, availableInventory - consumedQty);
                    
                    // Update total inventory level after consumption
                    currentTotal = currentInventory.values().stream().mapToDouble(Double::doubleValue).sum();
                    totalInventoryLevels.add(currentTotal);
                }
            }
        }
        
        // Calculate inventory fluctuation (variance of total inventory levels)
        double inventoryFluctuation = calculateVariance(totalInventoryLevels);
        
        double[] objectives = {makespan, totalLateness, inventoryFluctuation};
        chromosome.setObjectives(objectives);
        
        return objectives;
    }
    
    private String extractMaterialIdFromOrder(ShopOrder order) {
        // Extract material ID from shop order ID (format: MATERIAL_DATE)
        String shopOrderId = order.getShopOrder();
        int underscoreIndex = shopOrderId.lastIndexOf('_');
        if (underscoreIndex > 0) {
            return shopOrderId.substring(0, underscoreIndex);
        }
        return shopOrderId; // Fallback
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
}