package com.iimsoft.scheduler.v5.material;

import com.iimsoft.scheduler.v5.model.RouterStep;
import com.iimsoft.scheduler.v5.model.ShopOrder;
import com.iimsoft.scheduler.v5.model.ProductionShift;
import com.iimsoft.scheduler.v5.scheduling.IRouterStepProvider;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * In-memory implementation of data loading functionality.
 * Replaces DatabaseLoader behavior for the in-memory material planning demo.
 * Only implements methods needed by SchedulingEngine.
 */
public class InMemoryLoader implements IRouterStepProvider {
    private final Map<String, List<RouterStep>> routerStepsMap;
    private final List<ShopOrder> shopOrders;
    
    public InMemoryLoader(Map<String, List<RouterStep>> routerStepsMap, List<ShopOrder> shopOrders) {
        this.routerStepsMap = routerStepsMap;
        this.shopOrders = shopOrders;
    }
    
    /**
     * Loads router steps for a given router BO.
     * Used by SchedulingEngine for determining production steps.
     * 
     * @param routerBo The router business object identifier
     * @return List of router steps for this router
     */
    public List<RouterStep> loadRouterSteps(String routerBo) {
        return routerStepsMap.getOrDefault(routerBo, new ArrayList<>());
    }
    
    /**
     * Loads all pending shop orders.
     * In the in-memory scenario, these are the virtual orders generated from material demands.
     * 
     * @return List of shop orders
     */
    public List<ShopOrder> loadPendingShopOrders() {
        return new ArrayList<>(shopOrders);
    }
    
    /**
     * Loads production shifts.
     * For the in-memory demo, returns a default shift configuration.
     * 
     * @return List of production shifts
     */
    public List<ProductionShift> loadProductionShifts() {
        List<ProductionShift> shifts = new ArrayList<>();
        
        // Add a default day shift (8 AM to 8 PM)
        shifts.add(new ProductionShift(
            "SHIFT_001",
            "DAY_SHIFT", 
            8 * 60,      // start time in minutes from midnight (8:00 AM)
            "AM",
            20 * 60,     // end time in minutes from midnight (8:00 PM)  
            "PM"
        ));
        
        return shifts;
    }
}