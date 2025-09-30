package com.iimsoft.scheduler.v5.material.model;

/**
 * Defines material production capabilities including which resource
 * can produce the material and at what rate.
 */
public class MaterialDefinition {
    private String materialId;
    private String resourceId;
    private double ratePerHour; // units per hour production rate
    
    public MaterialDefinition(String materialId, String resourceId, double ratePerHour) {
        this.materialId = materialId;
        this.resourceId = resourceId;
        this.ratePerHour = ratePerHour;
    }
    
    // Getters
    public String getMaterialId() { return materialId; }
    public String getResourceId() { return resourceId; }
    public double getRatePerHour() { return ratePerHour; }
    
    // Setters
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public void setRatePerHour(double ratePerHour) { this.ratePerHour = ratePerHour; }
    
    @Override
    public String toString() {
        return String.format("MaterialDefinition{materialId='%s', resourceId='%s', ratePerHour=%.2f}", 
                           materialId, resourceId, ratePerHour);
    }
}