package com.iimsoft.scheduler.v5.model;

// 4. 排程结果 - model/ScheduleResult.java
import java.time.LocalDateTime;
import java.util.List;

public class ScheduleResult {
    private String shopOrderBo;
    private List<StepSchedule> stepSchedules;
    
    public ScheduleResult(String shopOrderBo, List<StepSchedule> stepSchedules) {
        this.shopOrderBo = shopOrderBo;
        this.stepSchedules = stepSchedules;
    }
    
    // Getters
    public String getShopOrderBo() { return shopOrderBo; }
    public List<StepSchedule> getStepSchedules() { return stepSchedules; }
}
