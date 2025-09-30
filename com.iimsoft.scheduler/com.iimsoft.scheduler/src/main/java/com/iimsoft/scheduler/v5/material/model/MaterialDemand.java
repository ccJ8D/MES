package com.iimsoft.scheduler.v5.material.model;

import java.time.LocalDate;

/**
 * Represents a material demand for a specific date.
 * Used in in-memory material planning to specify what materials 
 * are needed and when.
 */
public class MaterialDemand {
    private String materialId;
    private LocalDate date;
    private double quantity;
    
    public MaterialDemand(String materialId, LocalDate date, double quantity) {
        this.materialId = materialId;
        this.date = date;
        this.quantity = quantity;
    }
    
    // Getters
    public String getMaterialId() { return materialId; }
    public LocalDate getDate() { return date; }
    public double getQuantity() { return quantity; }
    
    // Setters
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    
    @Override
    public String toString() {
        return String.format("MaterialDemand{materialId='%s', date=%s, quantity=%.2f}", 
                           materialId, date, quantity);
    }
}