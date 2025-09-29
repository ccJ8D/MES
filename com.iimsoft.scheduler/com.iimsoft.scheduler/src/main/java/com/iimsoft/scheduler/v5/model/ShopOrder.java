package com.iimsoft.scheduler.v5.model;


import java.time.LocalDateTime;

public class ShopOrder {
    private String shopOrder;
    private double qtyToBuild;
    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedCompDate;
    private String routerBo;
    private boolean pullSystem;
    
    public ShopOrder(String shopOrder, double qtyToBuild, 
                    LocalDateTime plannedStartDate, LocalDateTime plannedCompDate, 
                    String routerBo) {
        this.shopOrder = shopOrder;
        this.qtyToBuild = qtyToBuild;
        this.plannedStartDate = plannedStartDate;
        this.plannedCompDate = plannedCompDate;
        this.routerBo = routerBo;
        this.pullSystem = false; // 默认为推式系统
    }
    
    // Getters
    public String getShopOrder() { return shopOrder; }
    public double getQtyToBuild() { return qtyToBuild; }
    public LocalDateTime getPlannedStartDate() { return plannedStartDate; }
    public LocalDateTime getPlannedCompDate() { return plannedCompDate; }
    public String getRouterBo() { return routerBo; }
    public boolean isPullSystem() { return pullSystem; }
    
    public void setPullSystem(boolean pullSystem) { this.pullSystem = pullSystem; }
}