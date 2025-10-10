package com.iimsoft.scheduler.db;


// 9. 排程结果存储 - db/ScheduleRepository.java


import com.iimsoft.scheduler.model.ScheduleResult;
import com.iimsoft.scheduler.model.StepSchedule;

public class ScheduleRepository {


    
    public void saveSchedule(ScheduleResult result) {
        // 删除旧的排程结果

        
        // 插入新的排程结果
        String sql = "INSERT INTO mom_shop_order_schedule \n"
        + "(mom_shop_order_schedule_id, shop_order_bo, sequence, \n"
        + " router_step_bo, resource_bo, planned_qty, start_date, end_date)\n"
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        for (StepSchedule schedule : result.getStepSchedules()) {

        }
    }
}
