package com.iimsoft.scheduler.phase1.model;

import com.iimsoft.scheduler.phase1.service.ShiftCalendarService;
import com.iimsoft.scheduler.phase1.lot.LotBatch;
import com.iimsoft.scheduler.common.ScheduleTask;
import com.iimsoft.scheduler.phase1.service.RateService;
import com.iimsoft.scheduler.util.TimeAlignUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 子件批次：整点 backward。
 */
public class ChildTaskBuilder {

    public static class Result {
        public final List<ScheduleTask> childTasks;
        public final Map<Integer, Set<Integer>> parentToChildren;
        public final int nextTaskId;

        public Result(List<ScheduleTask> childTasks,
                      Map<Integer, Set<Integer>> parentToChildren,
                      int nextTaskId) {
            this.childTasks = childTasks;
            this.parentToChildren = parentToChildren;
            this.nextTaskId = nextTaskId;
        }
    }

    private final ShiftCalendarService calendar;
    private final RateService rateService;
    private final int defaultWorkCenterId;

    public ChildTaskBuilder(ShiftCalendarService calendar,
                            RateService rateService,
                            int defaultWorkCenterId) {
        this.calendar = calendar;
        this.rateService = rateService;
        this.defaultWorkCenterId = defaultWorkCenterId;
    }

    public Result build(List<LotBatch> batches, int startTaskId) {
        List<ScheduleTask> tasks = new ArrayList<ScheduleTask>();
        Map<Integer, Set<Integer>> parentToChildren = new HashMap<Integer, Set<Integer>>();
        int id = startTaskId;

        for (LotBatch b : batches) {
            BigDecimal hoursCeil = rateService.computeProcessHoursCeil(b.getQty());
            int intHours = hoursCeil.intValue();

            LocalDateTime due = b.getDueDate();
            due = TimeAlignUtil.ceilToHour(due);

            LocalDateTime start = calendar.subtractWholeHours(defaultWorkCenterId, due, intHours);
            start = TimeAlignUtil.ceilToHour(start);

            ScheduleTask child = new ScheduleTask(
                    id,
                    b.getItemId(),
                    b.getQty(),
                    hoursCeil,
                    start,
                    due,
                    new ArrayList<Integer>()
            );
            tasks.add(child);

            for (Integer parentTaskId : b.getParentQtyMap().keySet()) {
                Set<Integer> set = parentToChildren.get(parentTaskId);
                if (set == null) {
                    set = new LinkedHashSet<Integer>();
                    parentToChildren.put(parentTaskId, set);
                }
                set.add(id);
            }
            id++;
        }
        return new Result(tasks, parentToChildren, id);
    }
}