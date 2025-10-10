package com.iimsoft.scheduler.model;

// 6. 生产班次 - model/ProductionShift.java

public class ProductionShift {
    private String id;
    private String productionShift;
    private int startTime;
    private String startTimeType;
    private int endTime;
    private String endTimeType;
    
    public ProductionShift(String id, String productionShift, int startTime, 
                          String startTimeType, int endTime, String endTimeType) {
        this.id = id;
        this.productionShift = productionShift;
        this.startTime = startTime;
        this.startTimeType = startTimeType;
        this.endTime = endTime;
        this.endTimeType = endTimeType;
    }
    
    // Getters
    public String getId() { return id; }
    public String getProductionShift() { return productionShift; }
    public int getStartTime() { return startTime; }
    public String getStartTimeType() { return startTimeType; }
    public int getEndTime() { return endTime; }
    public String getEndTimeType() { return endTimeType; }
}
