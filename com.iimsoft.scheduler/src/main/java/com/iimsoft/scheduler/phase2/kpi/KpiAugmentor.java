package com.iimsoft.scheduler.phase2.kpi;

import com.iimsoft.scheduler.common.ScheduleTask;

import java.util.List;

/**
 * 允许业务在默认 KPI 收集后扩展自定义指标。
 */
public interface KpiAugmentor {
    void augment(List<ScheduleTask> tasks, KpiReport report);
}