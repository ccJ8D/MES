package com.iimsoft.scheduler.v5.material.model;

import java.time.LocalDate;

/**
 * Represents available production capacity for a resource on a specific date.
 * Used to calculate resource utilization and identify bottlenecks.
 */
public class MaterialCapacity {
    private String resourceId;
    private LocalDate date;
    private double availableHours;
    
    public MaterialCapacity(String resourceId, LocalDate date, double availableHours) {
        this.resourceId = resourceId;
        this.date = date;
        this.availableHours = availableHours;
    }
    
    // Getters
    public String getResourceId() { return resourceId; }
    public LocalDate getDate() { return date; }
    public double getAvailableHours() { return availableHours; }
    
    // Setters
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setAvailableHours(double availableHours) { this.availableHours = availableHours; }
    
    @Override
    public String toString() {
        return String.format("MaterialCapacity{resourceId='%s', date=%s, availableHours=%.2f}", 
                           resourceId, date, availableHours);
    }
}