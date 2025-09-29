package com.iimsoft.scheduler.v5.db;


// 9. 排程结果存储 - db/ScheduleRepository.java


import com.iimsoft.scheduler.v5.model.ScheduleResult;
import com.iimsoft.scheduler.v5.model.StepSchedule;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Timestamp;
import java.util.UUID;

public class ScheduleRepository {
    private final JdbcTemplate jdbcTemplate;
    
    public ScheduleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public void saveSchedule(ScheduleResult result) {
        // 删除旧的排程结果
        jdbcTemplate.update(
            "DELETE FROM mom_shop_order_schedule WHERE shop_order_bo = ?", 
            result.getShopOrderBo()
        );
        
        // 插入新的排程结果
        String sql = "INSERT INTO mom_shop_order_schedule \n"
        + "(mom_shop_order_schedule_id, shop_order_bo, sequence, \n"
        + " router_step_bo, resource_bo, planned_qty, start_date, end_date)\n"
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        for (StepSchedule schedule : result.getStepSchedules()) {
            jdbcTemplate.update(sql,
                UUID.randomUUID().toString(),
                result.getShopOrderBo(),
                schedule.getSequence(),
                schedule.getRouterStepBo(),
                schedule.getResourceBo(),
                schedule.getPlannedQty(),
                Timestamp.valueOf(schedule.getStartTime()),
                Timestamp.valueOf(schedule.getEndTime())
            );
        }
    }
}
