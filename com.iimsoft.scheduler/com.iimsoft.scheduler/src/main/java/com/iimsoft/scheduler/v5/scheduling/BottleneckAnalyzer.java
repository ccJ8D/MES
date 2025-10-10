package com.iimsoft.scheduler.v5.scheduling;

// 12. TOC瓶颈分析器（已改为按 router 聚合） - scheduling/BottleneckAnalyzer.java

import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;
import java.sql.Timestamp;
import java.util.*;

public class BottleneckAnalyzer {
    private final JdbcTemplate jdbcTemplate;
    private final ResourceCalendar calendar;
    
    public BottleneckAnalyzer(JdbcTemplate jdbcTemplate, ResourceCalendar calendar) {
        this.jdbcTemplate = jdbcTemplate;
        this.calendar = calendar;
    }
    
    /**
     * 返回被识别为瓶颈的 router_bo（工艺路线）
     */
    public String identifyBottleneck(LocalDate startDate, LocalDate endDate) {
        List<String> routerBos = loadAllRouterBos();
        String bottleneck = null;
        double maxScore = Double.NEGATIVE_INFINITY;
        
        for (String routerBo : routerBos) {
            double loadFactor = calculateLoadFactor(routerBo, startDate, endDate);
            double utilization = calculateUtilizationFactor(routerBo);
            double changeover = calculateChangeoverFactor(routerBo, startDate, endDate);
            
            // 权重可以根据业务调整
            double score = loadFactor * 0.6 + utilization * 0.3 + changeover * 0.1;
            
            if (score > maxScore) {
                maxScore = score;
                bottleneck = routerBo;
            }
        }
        
        return bottleneck;
    }
    
    private List<String> loadAllRouterBos() {
        String sql = "SELECT DISTINCT router_bo FROM mom_router_step WHERE router_bo IS NOT NULL";
        return jdbcTemplate.queryForList(sql, String.class);
    }
    
    /**
     * 负载因子：统计属于该 router 的所有待排工单的预计总工时 / 可用分钟（取 0..1）
     * 这里把每个工单在该 router 上所有步骤的时间求和作为工单对该 router 的负载贡献。
     */
    private double calculateLoadFactor(String routerBo, LocalDate start, LocalDate end) {
        long availableMinutes = calendar.getAvailableMinutes(routerBo, start, end);
        if (availableMinutes == 0) return 0;
        
        // 统计属于该 router 的工单在该 router 上的总预计时间（分钟）
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
            + "WHERE so.status_bo IN ('Released', 'InProgress')\n"
            + "  AND rs.router_bo = ?\n"
            + "  AND so.planned_start_date BETWEEN ? AND ?";
        
        Timestamp tsStart = Timestamp.valueOf(start.atStartOfDay());
        Timestamp tsEnd = Timestamp.valueOf(end.atTime(23, 59, 59));
        
        Double totalLoad = jdbcTemplate.queryForObject(sql, Double.class,
            routerBo, tsStart, tsEnd);
        
        double loadMinutes = totalLoad != null ? totalLoad : 0.0;
        return Math.min(loadMinutes / availableMinutes, 1.0);
    }
    
    /**
     * 利用率因子：对属于 router 的所有涉及 resource 的历史利用率求平均
     * 若没有 router 级表，则聚合其 resource 的历史利用率。
     */
    private double calculateUtilizationFactor(String routerBo) {
        // 找到该 router 涉及的 resource 列表
        String resSql = "SELECT DISTINCT op.resource_bo \n"
            + "FROM mom_router_step rs\n"
            + "JOIN mom_router_operation ro ON rs.handle = ro.router_step_bo\n"
            + "JOIN mom_operation op ON ro.operation_bo = op.handle\n"
            + "WHERE rs.router_bo = ?";
        
        List<String> resources = jdbcTemplate.queryForList(resSql, new Object[]{routerBo}, String.class);
        if (resources == null || resources.isEmpty()) {
            return 0.7; // 兜底值
        }
        
        // 聚合这些资源的历史利用率均值（过去90天）
        String inClause = String.join(",", Collections.nCopies(resources.size(), "?"));
        String utilSql = "SELECT AVG(utilization) FROM resource_utilization_history\n"
            + "WHERE resource_id IN (" + inClause + ") AND date > CURRENT_DATE - INTERVAL '90 days'";
        
        List<Object> params = new ArrayList<>(resources);
        Double avgUtil = jdbcTemplate.queryForObject(utilSql, params.toArray(), Double.class);
        return avgUtil != null ? avgUtil / 100.0 : 0.7;
    }
    
    /**
     * 换线因子：聚合 router 相关资源在时间窗内的换线时间占比
     */
    private double calculateChangeoverFactor(String routerBo, LocalDate start, LocalDate end) {
        String resSql = "SELECT DISTINCT op.resource_bo \n"
            + "FROM mom_router_step rs\n"
            + "JOIN mom_router_operation ro ON rs.handle = ro.router_step_bo\n"
            + "JOIN mom_operation op ON ro.operation_bo = op.handle\n"
            + "WHERE rs.router_bo = ?";
        
        List<String> resources = jdbcTemplate.queryForList(resSql, new Object[]{routerBo}, String.class);
        if (resources == null || resources.isEmpty()) return 0.0;
        
        String inClause = String.join(",", Collections.nCopies(resources.size(), "?"));
        String coSql = "SELECT COALESCE(SUM(changeover_time), 0)\n"
            + "FROM resource_changeover\n"
            + "WHERE resource_id IN (" + inClause + ") AND changeover_date BETWEEN ? AND ?";
        
        List<Object> params = new ArrayList<>(resources);
        params.add(Timestamp.valueOf(start.atStartOfDay()));
        params.add(Timestamp.valueOf(end.atTime(23, 59, 59)));
        
        Double totalChangeover = jdbcTemplate.queryForObject(coSql, params.toArray(), Double.class);
        long availableMinutes = calendar.getAvailableMinutes(routerBo, start, end);
        if (availableMinutes == 0) return 0.0;
        return Math.min((totalChangeover != null ? totalChangeover : 0) / availableMinutes, 1.0);
    }
}