package com.iimsoft.scheduler.v5.material.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Configuration for material planning including planning horizon,
 * material demands, definitions, capacities, and initial inventories.
 */
public class MaterialPlanConfig {
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime plannedStartTime = LocalTime.of(8, 0); // Default 8:00 AM
    private LocalTime plannedCompTime = LocalTime.of(23, 59, 59); // Default 11:59:59 PM
    
    private List<MaterialDemand> demands;
    private List<MaterialDefinition> definitions;
    private List<MaterialCapacity> capacities;
    private Map<String, Double> initialInventory;
    
    public MaterialPlanConfig(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    // Getters
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalTime getPlannedStartTime() { return plannedStartTime; }
    public LocalTime getPlannedCompTime() { return plannedCompTime; }
    public List<MaterialDemand> getDemands() { return demands; }
    public List<MaterialDefinition> getDefinitions() { return definitions; }
    public List<MaterialCapacity> getCapacities() { return capacities; }
    public Map<String, Double> getInitialInventory() { return initialInventory; }
    
    // Setters
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setPlannedStartTime(LocalTime plannedStartTime) { this.plannedStartTime = plannedStartTime; }
    public void setPlannedCompTime(LocalTime plannedCompTime) { this.plannedCompTime = plannedCompTime; }
    public void setDemands(List<MaterialDemand> demands) { this.demands = demands; }
    public void setDefinitions(List<MaterialDefinition> definitions) { this.definitions = definitions; }
    public void setCapacities(List<MaterialCapacity> capacities) { this.capacities = capacities; }
    public void setInitialInventory(Map<String, Double> initialInventory) { this.initialInventory = initialInventory; }
    
    @Override
    public String toString() {
        return String.format("MaterialPlanConfig{startDate=%s, endDate=%s, demands=%d, definitions=%d, capacities=%d, initialInventory=%d}", 
                           startDate, endDate, 
                           demands != null ? demands.size() : 0,
                           definitions != null ? definitions.size() : 0,
                           capacities != null ? capacities.size() : 0,
                           initialInventory != null ? initialInventory.size() : 0);
    }
}