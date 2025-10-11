package com.iimsoft.scheduler.nsga;

import com.iimsoft.scheduler.model.ScheduleTask;
import java.util.List;

/**
 * 单目标接口。
 * evaluate 返回一个数值（越小越好默认，可在实现中统一）
 */
public interface ScheduleObjective {
    double evaluate(List<ScheduleTask> tasks);
    String name();
}