package com.iimsoft.scheduler.builder;

import com.iimsoft.scheduler.model.DailyDemand;
import com.iimsoft.scheduler.model.ScheduleTask;
import com.iimsoft.scheduler.service.RateService;
import com.iimsoft.scheduler.shift.WorkCalendarService;
import com.iimsoft.scheduler.util.TimeAlignUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 顶层任务：使用整点工时 (ceil) + 整点 backward。
 */
public class TopLevelTaskBuilder {

    private final WorkCalendarService calendar;
    private final RateService rateService;
    private final int workCenterId;

    public TopLevelTaskBuilder(WorkCalendarService calendar,
                               RateService rateService,
                               int workCenterId) {
        this.calendar = calendar;
        this.rateService = rateService;
        this.workCenterId = workCenterId;
    }

    public List<ScheduleTask> build(List<DailyDemand> demands) {
        List<ScheduleTask> tasks = new ArrayList<ScheduleTask>();
        int id = 1;
        for (DailyDemand d : demands) {
            if (d.getQuantity() == null || d.getQuantity().signum() <= 0) continue;
            LocalDate day = d.getDay();

            BigDecimal hoursCeil = rateService.computeProcessHoursCeil(d.getQuantity());
            int h = hoursCeil.intValue();
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
            tasks.add(task);
        }
        return tasks;
    }
}