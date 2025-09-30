package com.iimsoft.scheduler.v5.material;

import com.iimsoft.scheduler.v5.material.model.*;
import com.iimsoft.scheduler.v5.model.ShopOrder;
import com.iimsoft.scheduler.v5.scheduling.ResourceCalendar;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory implementation of bottleneck analysis.
 * Reuses scoring formula from BottleneckAnalyzer but uses in-memory data
 * instead of database queries.
 */
public class InMemoryBottleneckAnalyzer {
    private final MaterialPlanConfig config;
    private final List<ShopOrder> virtualOrders;
    private final ResourceCalendar calendar;
    
    // Default constants for in-memory analysis
    private static final double DEFAULT_UTILIZATION = 0.7; // Default utilization factor
    private static final double DEFAULT_CHANGEOVER_FACTOR = 0.1; // Minimal changeover for MVP
    
    public InMemoryBottleneckAnalyzer(MaterialPlanConfig config, List<ShopOrder> virtualOrders, ResourceCalendar calendar) {
        this.config = config;
        this.virtualOrders = virtualOrders;
        this.calendar = calendar;
    }
    
    /**
     * Identifies the bottleneck resource using in-memory calculations.
     * 
     * @return The resource ID that is the bottleneck
     */
    public String identifyBottleneck() {
        List<String> resourceIds = getAllResourceIds();
        String bottleneck = null;
        double maxScore = 0.0;
        
        System.out.println("=== Bottleneck Analysis ===");
        
        for (String resourceId : resourceIds) {
            double loadFactor = calculateLoadFactor(resourceId);
            double utilizationFactor = DEFAULT_UTILIZATION; // Use constant for in-memory demo
            double changeoverFactor = calculateChangeoverFactor(resourceId);
            
            double score = loadFactor * 0.6 + utilizationFactor * 0.3 + changeoverFactor * 0.1;
            
            System.out.printf("Resource %s: Load=%.3f, Util=%.3f, Changeover=%.3f, Score=%.3f%n", 
                            resourceId, loadFactor, utilizationFactor, changeoverFactor, score);
            
            if (score > maxScore) {
                maxScore = score;
                bottleneck = resourceId;
            }
        }
        
        System.out.printf("Identified bottleneck: %s (score: %.3f)%n", bottleneck, maxScore);
        return bottleneck;
    }
    
    private List<String> getAllResourceIds() {
        return config.getDefinitions().stream()
            .map(MaterialDefinition::getResourceId)
            .distinct()
            .collect(Collectors.toList());
    }
    
    private double calculateLoadFactor(String resourceId) {
        // Calculate total required minutes for this resource
        double totalRequiredMinutes = 0.0;
        
        // Get material definition for this resource
        Map<String, MaterialDefinition> materialsByResource = config.getDefinitions().stream()
            .filter(def -> def.getResourceId().equals(resourceId))
            .collect(Collectors.toMap(MaterialDefinition::getMaterialId, def -> def));
        
        // Sum up required time for all virtual orders using this resource
        for (ShopOrder order : virtualOrders) {
            String materialId = extractMaterialIdFromOrder(order);
            MaterialDefinition definition = materialsByResource.get(materialId);
            
            if (definition != null && definition.getRatePerHour() > 0) {
                // Calculate required minutes: (quantity / rate_per_hour) * 60
                double requiredMinutes = (order.getQtyToBuild() / definition.getRatePerHour()) * 60.0;
                totalRequiredMinutes += requiredMinutes;
            }
        }
        
        // Calculate total available minutes for this resource
        double totalAvailableMinutes = 0.0;
        for (MaterialCapacity capacity : config.getCapacities()) {
            if (capacity.getResourceId().equals(resourceId)) {
                totalAvailableMinutes += capacity.getAvailableHours() * 60.0; // Convert to minutes
            }
        }
        
        if (totalAvailableMinutes == 0) return 0.0;
        
        // Load factor = required / available (capped at 1.0)
        return Math.min(totalRequiredMinutes / totalAvailableMinutes, 1.0);
    }
    
    private double calculateChangeoverFactor(String resourceId) {
        // For MVP, use a simplified changeover calculation
        // Count material switches in chronological order
        List<String> materialSequence = virtualOrders.stream()
            .filter(order -> {
                String materialId = extractMaterialIdFromOrder(order);
                return config.getDefinitions().stream()
                    .anyMatch(def -> def.getMaterialId().equals(materialId) && 
                             def.getResourceId().equals(resourceId));
            })
            .sorted(Comparator.comparing(ShopOrder::getPlannedStartDate))
            .map(this::extractMaterialIdFromOrder)
            .collect(Collectors.toList());
        
        if (materialSequence.size() <= 1) return 0.0;
        
        int changeovers = 0;
        for (int i = 1; i < materialSequence.size(); i++) {
            if (!materialSequence.get(i).equals(materialSequence.get(i-1))) {
                changeovers++;
            }
        }
        
        // Normalize by number of possible changeovers
        return (double) changeovers / (materialSequence.size() - 1);
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
}