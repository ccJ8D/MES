package com.iimsoft.scheduler.v5.db;

// 10. 库存曲线存储 - db/InventoryCurveRepository.java


import com.iimsoft.scheduler.v5.optimization.ObjectiveCalculator.InventoryPoint;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;

public class InventoryCurveRepository {
    private final JdbcTemplate jdbcTemplate;
    
    public InventoryCurveRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public void saveCurve(long solutionId, List<InventoryPoint> curve) {
        String sql = "INSERT INTO mom_schedule_inventory_curve \n"
        + "(solution_id, timestamp, inventory_level, note)\n"
        + "VALUES (?, ?, ?, ?)";
            
        for (InventoryPoint point : curve) {
            jdbcTemplate.update(sql,
                solutionId,
                new java.sql.Timestamp(point.timestamp.getTime()),
                point.level,
                point.note
            );
        }
    }
}