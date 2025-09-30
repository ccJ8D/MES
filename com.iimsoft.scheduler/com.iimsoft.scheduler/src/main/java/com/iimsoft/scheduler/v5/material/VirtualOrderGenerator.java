package com.iimsoft.scheduler.v5.material;

import com.iimsoft.scheduler.v5.material.model.*;
import com.iimsoft.scheduler.v5.model.ShopOrder;
import com.iimsoft.scheduler.v5.model.RouterStep;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates virtual shop orders from material demands.
 * Creates one shop order per material-date combination with synthetic routing.
 */
public class VirtualOrderGenerator {
    
    /**
     * Converts material demands into virtual shop orders.
     * 
     * @param config The material planning configuration
     * @return List of virtual shop orders
     */
    public List<ShopOrder> generateVirtualOrders(MaterialPlanConfig config) {
        List<ShopOrder> orders = new ArrayList<>();
        
        // Create a map for quick lookup of material definitions
        Map<String, MaterialDefinition> definitionMap = config.getDefinitions().stream()
            .collect(Collectors.toMap(MaterialDefinition::getMaterialId, def -> def));
        
        for (MaterialDemand demand : config.getDemands()) {
            MaterialDefinition definition = definitionMap.get(demand.getMaterialId());
            if (definition == null) {
                System.err.println("Warning: No definition found for material " + demand.getMaterialId());
                continue;
            }
            
            // Create unique shop order ID
            String shopOrderId = String.format("%s_%s", demand.getMaterialId(), demand.getDate().toString());
            
            // Set planned times
            LocalDateTime plannedStart = demand.getDate().atTime(config.getPlannedStartTime());
            LocalDateTime plannedComp = demand.getDate().atTime(config.getPlannedCompTime());
            
            // Create router BO
            String routerBo = demand.getMaterialId() + "_ROUTER";
            
            ShopOrder order = new ShopOrder(
                shopOrderId,
                demand.getQuantity(),
                plannedStart,
                plannedComp,
                routerBo
            );
            
            orders.add(order);
        }
        
        return orders;
    }
    
    /**
     * Generates synthetic router steps for virtual orders.
     * Creates one step per material using the mapped resource.
     * 
     * @param config The material planning configuration
     * @return Map of router BO to list of router steps
     */
    public Map<String, List<RouterStep>> generateRouterSteps(MaterialPlanConfig config) {
        Map<String, MaterialDefinition> definitionMap = config.getDefinitions().stream()
            .collect(Collectors.toMap(MaterialDefinition::getMaterialId, def -> def));
        
        return config.getDemands().stream()
            .map(demand -> demand.getMaterialId())
            .distinct()
            .collect(Collectors.toMap(
                materialId -> materialId + "_ROUTER",
                materialId -> {
                    MaterialDefinition definition = definitionMap.get(materialId);
                    List<RouterStep> steps = new ArrayList<>();
                    
                    if (definition != null) {
                        RouterStep step = new RouterStep(
                            materialId + "_STEP_001",  // handle
                            1,                         // sequence
                            "PRODUCE_" + materialId,   // operation
                            definition.getResourceId() // resource BO
                        );
                        steps.add(step);
                    }
                    
                    return steps;
                }
            ));
    }
}