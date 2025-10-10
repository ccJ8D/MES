package com.iimsoft.scheduler.scheduling;

// 12. TOC瓶颈分析器 - scheduling/BottleneckAnalyzer.java


import java.time.LocalDate;
import java.util.*;

public class BottleneckAnalyzer {

    private final ResourceCalendar calendar;
    
    public BottleneckAnalyzer( ResourceCalendar calendar) {

        this.calendar = calendar;
    }
    
    public String identifyBottleneck(LocalDate startDate, LocalDate endDate) {
//        List<String> resourceIds = loadAllResourceIds();
//        String bottleneck = null;
//        double maxScore = 0.0;
//
//        for (String resourceId : resourceIds) {
//            double loadFactor = calculateLoadFactor(resourceId, startDate, endDate);
//            double utilization = calculateUtilizationFactor(resourceId);
//            double changeover = calculateChangeoverFactor(resourceId, startDate, endDate);
//
//            double score = loadFactor * 0.6 + utilization * 0.3 + changeover * 0.1;
//
//            if (score > maxScore) {
//                maxScore = score;
//                bottleneck = resourceId;
//            }
//        }
        
        return "--";
    }
    
    private List<String> loadAllResourceIds() {
        String sql = "SELECT DISTINCT resource_bo FROM mom_operation WHERE resource_bo IS NOT NULL";
        return null;
    }
    
    private double calculateLoadFactor(String resourceId, LocalDate start, LocalDate end) {
        long availableMinutes = calendar.getAvailableMinutes(resourceId, start, end);
        if (availableMinutes == 0) return 0;
        
        String sql = "SELECT COALESCE(SUM(\n"
        + "    CASE \n"
        + "        WHEN ssr.standard_rate_qty > 0 AND ssr.standard_rate_time > 0\n"
        + "        THEN (so.qty_to_build / ssr.standard_rate_qty) * ssr.standard_rate_time\n"
        + "        ELSE op.required_time_in_process * so.qty_to_build\n"
        + "    END\n"
        + "), 0) AS total_load\n"
        + "FROM mom_shop_order so\n"
        + "JOIN mom_router_step rs ON so.router_bo = rs.router_bo\n"
        + "JOIN mom_router_operation ro ON rs.handle = ro.router_step_bo\n"
        + "JOIN mom_operation op ON ro.operation_bo = op.handle\n"
        + "LEFT JOIN mom_shop_order_standard_rate ssr \n"
        + "    ON so.handle = ssr.shop_order_bo \n"
        + "    AND ro.operation_bo = ssr.operation_bo\n"
        + "    AND ssr.resource_bo = ?\n"
        + "WHERE so.status_bo IN ('Released', 'InProgress')\n"
        + "    AND op.resource_bo = ?\n"
        + "    AND so.planned_start_date BETWEEN ? AND ?";
            
        Double totalLoad = 0.0;
        
        return Math.min((totalLoad != null ? totalLoad : 0) / availableMinutes, 1.0);
    }
    
    private double calculateUtilizationFactor(String resourceId) {

        String sql = "SELECT AVG(utilization) FROM resource_utilization_history\n"
        + "WHERE resource_id = ? AND date > CURRENT_DATE - INTERVAL '90 days'";
        
        Double avgUtilization = 1.0;
        return avgUtilization != null ? avgUtilization / 100.0 : 0.7;
    }
    
    private double calculateChangeoverFactor(String resourceId, LocalDate start, LocalDate end) {
        String sql = "SELECT COALESCE(SUM(changeover_time), 0)\n"
        + "FROM resource_changeover\n"
        + "WHERE resource_id = ? AND changeover_date BETWEEN ? AND ? ";
        
        Double totalChangeover = 1.0;
        
        long availableMinutes = calendar.getAvailableMinutes(resourceId, start, end);
        if (availableMinutes == 0) return 0;
        
        return Math.min((totalChangeover != null ? totalChangeover : 0) / availableMinutes, 1.0);
    }
}