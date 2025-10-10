package com.iimsoft.scheduler.model;

// 5. 步骤排程 - model/StepSchedule.java

import java.time.LocalDateTime;

public class StepSchedule {
    private int sequence;
    private String routerStepBo;
    private String resourceBo;
    private double plannedQty;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    public StepSchedule(int sequence, String routerStepBo, String resourceBo,
                       double plannedQty, LocalDateTime startTime, LocalDateTime endTime) {
        this.sequence = sequence;
        this.routerStepBo = routerStepBo;
        this.resourceBo = resourceBo;
        this.plannedQty = plannedQty;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    // Getters
    public int getSequence() { return sequence; }
    public String getRouterStepBo() { return routerStepBo; }
    public String getResourceBo() { return resourceBo; }
    public double getPlannedQty() { return plannedQty; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
}