package com.iimsoft.scheduler.phase1.service;

import com.iimsoft.scheduler.common.DailyDemand;
import com.iimsoft.scheduler.common.ScheduleTask;
import com.iimsoft.scheduler.util.ShiftUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 顶层任务：使用整点工时 (ceil) + 整点 backward。
 * 顶层任务构建器
 */
public class TopLevelTaskBuilder {

    private final ShiftCalendarService calendar;
    private final RateService rateService;
    private int workCenterId;

    public TopLevelTaskBuilder(ShiftCalendarService calendar,
                               RateService rateService) {
        this.calendar = calendar;
        this.rateService = rateService;
    }

    public List<ScheduleTask> build(List<DailyDemand> demands) {
        List<ScheduleTask> tasks = new ArrayList<ScheduleTask>();
        int id = 1;
        for (DailyDemand d : demands) {
            if (d.getQuantity() == null || d.getQuantity().signum() <= 0) continue;
            LocalDate day = d.getDay();

            BigDecimal hoursCeil = rateService.computeProcessHoursCeil(d.getQuantity());
            int h = hoursCeil.intValue();

            workCenterId = ShiftUtil.getLineByItem(d.getItemId()); // 每个顶层件可能不同
            calendar.registerDailyTemplate(workCenterId);

            LocalDateTime due = calendar.lastShiftEnd(workCenterId, day);
            LocalDateTime start = calendar.subtractWholeHours(workCenterId, due, h);
            LocalDateTime end = calendar.addWholeHours(workCenterId, start, h); // 理论 = due 或 <= due

            ScheduleTask task = new ScheduleTask(
                    id++,
                    d.getItemId(),
                    d.getQuantity(),
                    hoursCeil,          // 整数小时
                    start,
                    due,
                    new ArrayList<Integer>()
            );
            task.setWorkCenterId(workCenterId);
            tasks.add(task);
        }
        return tasks;
    }
}