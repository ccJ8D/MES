package com.iimsoft.scheduler.model;

// 4. 排程结果 - model/ScheduleResult.java
import java.util.List;

public class ScheduleResult {
    private Integer shopOrderBo;
    private List<StepSchedule> stepSchedules;
    
    public ScheduleResult(Integer shopOrderBo, List<StepSchedule> stepSchedules) {
        this.shopOrderBo = shopOrderBo;
        this.stepSchedules = stepSchedules;
    }
    
    // Getters
    public Integer getShopOrderBo() { return shopOrderBo; }
    public List<StepSchedule> getStepSchedules() { return stepSchedules; }
}
