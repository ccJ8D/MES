package com.iimsoft.scheduler.v5.db;

// 8. 数据库加载器 - db/DatabaseLoader.java

import org.springframework.jdbc.core.JdbcTemplate;
import com.iimsoft.scheduler.v5.model.*;
import com.iimsoft.scheduler.v5.scheduling.IRouterStepProvider;

import java.util.*;

public class DatabaseLoader implements IRouterStepProvider {
    private final JdbcTemplate jdbcTemplate;
    
    public DatabaseLoader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public List<ShopOrder> loadPendingShopOrders() {
        String sql = "SELECT shop_order, qty_to_build, planned_start_date, \n"
        + "       planned_comp_date, router_bo \n"
        + "FROM mom_shop_order \n"
        + "WHERE status_bo IN ('Released', 'InProgress')";

            
        return jdbcTemplate.query(sql, (rs, rowNum) -> 
            new ShopOrder(
                rs.getString("shop_order"),
                rs.getDouble("qty_to_build"),
                rs.getTimestamp("planned_start_date").toLocalDateTime(),
                rs.getTimestamp("planned_comp_date").toLocalDateTime(),
                rs.getString("router_bo")
            )
        );
    }
    
    public List<RouterStep> loadRouterSteps(String routerBo) {
        String sql = "SELECT rs.handle, rs.sequence, op.operation, op.resource_bo\n"
        + "FROM mom_router_step rs\n"
        + "JOIN mom_router_operation ro ON rs.handle = ro.router_step_bo\n"
        + "JOIN mom_operation op ON ro.operation_bo = op.handle\n"
        + "WHERE rs.router_bo = ?\n"
        + "ORDER BY rs.sequence";
            
        return jdbcTemplate.query(sql, (rs, rowNum) -> 
            new RouterStep(
                rs.getString("handle"),
                rs.getInt("sequence"),
                rs.getString("operation"),
                rs.getString("resource_bo")
            ), routerBo);
    }
    
    public List<ProductionShift> loadProductionShifts() {
        String sql = "SELECT * FROM mom_production_shift";
        return jdbcTemplate.query(sql, (rs, rowNum) -> 
            new ProductionShift(
                rs.getString("mom_production_shift_id"),
                rs.getString("production_shift"),
                rs.getInt("start_time"),
                rs.getString("start_time_type"),
                rs.getInt("end_time"),
                rs.getString("end_time_type")
            )
        );
    }
    
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
}