package com.iimsoft.scheduler.phase1.model;

import com.iimsoft.scheduler.common.DailyDemand;
import com.iimsoft.scheduler.common.ScheduleTask;
import com.iimsoft.scheduler.phase0.RateResolver;
import com.iimsoft.scheduler.phase0.WorkCenterResolver;
import com.iimsoft.scheduler.phase1.shift.ShiftCalendarService;

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
    private final RateResolver rateResolver;
    private final WorkCenterResolver workCenterResolver;

    public TopLevelTaskBuilder(ShiftCalendarService calendar,
                               RateResolver rateResolver,
                               WorkCenterResolver workCenterResolver) {
        this.calendar = calendar;
        this.rateResolver = rateResolver;
        this.workCenterResolver = workCenterResolver;
    }

    public List<ScheduleTask> build(List<DailyDemand> demands) {
        List<ScheduleTask> tasks = new ArrayList<ScheduleTask>();
        int id = 1;
        for (DailyDemand d : demands) {
            if (d.getQuantity() == null || d.getQuantity().signum() <= 0) continue;
            LocalDate day = d.getDay();
            BigDecimal rateForItem = rateResolver.getRateForItem(d.getItemId());
            BigDecimal hoursCeil = rateResolver.computeProcessHoursCeil(d.getQuantity(),rateForItem);
            int h = hoursCeil.intValue();

            int wcId = 0;
            if (workCenterResolver != null) {
                wcId = workCenterResolver.getWorkCenterId(d.getItemId());
            }
            if (wcId <= 0) {
                wcId = 0;
            }
            LocalDateTime due = calendar.lastShiftEnd(wcId, day);  // 当天最后班次结束
            LocalDateTime start = calendar.subtractWholeHours(wcId, due, h); // 向前推 h 小时
            LocalDateTime end = calendar.addWholeHours(wcId, start, h); // 验证 end

            ScheduleTask task = new ScheduleTask(
                    id++,
                    d.getItemId(),
                    d.getQuantity(),
                    hoursCeil,
                    start,
                    due,
                    wcId,
                    new ArrayList<Integer>()
            );
            task.setOriginalBackwardStart(start); // 记录原 backward start
            tasks.add(task);
        }
        return tasks;
    }
}