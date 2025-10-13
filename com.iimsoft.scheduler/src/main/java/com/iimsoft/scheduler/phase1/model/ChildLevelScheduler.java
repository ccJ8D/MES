package com.iimsoft.scheduler.phase1.model;

import com.iimsoft.scheduler.phase1.lot.LotBatch;
import com.iimsoft.scheduler.phase1.lot.LotSplitContext;
import com.iimsoft.scheduler.phase1.lot.LotSplitStrategy;
import com.iimsoft.scheduler.common.ScheduleTask;

import com.iimsoft.scheduler.phase1.service.RateService;
import com.iimsoft.scheduler.phase1.service.ShiftCalendarService;
import com.iimsoft.scheduler.util.ShiftUtil;
import com.iimsoft.scheduler.util.TimeAlignUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 单“层”调度：把该层所有直接子件贡献（已经确定 requiredBy）→ 拆批 → backward
 */
public class ChildLevelScheduler {

    public static class LayerResult {
        public final List<ScheduleTask> newChildTasks;
        public final Map<Integer, Set<Integer>> parentToChildren;
        public LayerResult(List<ScheduleTask> newChildTasks,
                           Map<Integer, Set<Integer>> parentToChildren) {
            this.newChildTasks = newChildTasks;
            this.parentToChildren = parentToChildren;
        }
    }

    private final ShiftCalendarService calendar;
    private final RateService rateService;
    private final LotSplitStrategy lotSplitStrategy;


    public ChildLevelScheduler(ShiftCalendarService calendar,
                               RateService rateService,
                               LotSplitStrategy lotSplitStrategy
                                ) {
        this.calendar = calendar;
        this.rateService = rateService;
        this.lotSplitStrategy = lotSplitStrategy;

    }

    public LayerResult scheduleLevel(List<LevelContribution> contributions,
                                     int startTaskId) {
        if (contributions.isEmpty()) {
            return new LayerResult(Collections.emptyList(),
                    Collections.emptyMap());
        }
        // 1. 聚合同一子件
        Map<Integer, List<LevelContribution>> byItem = new HashMap<>();
        for (LevelContribution c : contributions) {
            List<LevelContribution> list = byItem.get(c.getChildItemId());
            if (list == null) {
                list = new ArrayList<LevelContribution>();
                byItem.put(c.getChildItemId(), list);
            }
            list.add(c);
        }

        List<ScheduleTask> childTasks = new ArrayList<>();
        Map<Integer, Set<Integer>> parentToChildren = new HashMap<>();
        int nextId = startTaskId;

        for (Map.Entry<Integer, List<LevelContribution>> entry : byItem.entrySet()) {
            int childItemId = entry.getKey();
            List<LevelContribution> list = entry.getValue();

            // 统计
            BigDecimal total = BigDecimal.ZERO;
            LocalDateTime earliest = null;
            LocalDateTime latest = null;
            int level = 0;
            for (LevelContribution c : list) {
                total = total.add(c.getQuantity());
                if (earliest == null || c.getRequiredBy().isBefore(earliest)) earliest = c.getRequiredBy();
                if (latest == null || c.getRequiredBy().isAfter(latest)) latest = c.getRequiredBy();
                if (c.getLevel() > level) level = c.getLevel();
            }
            LotSplitContext ctx = new LotSplitContext(
                    childItemId, total, earliest, latest, level, list);
            List<LotBatch> batches = lotSplitStrategy.split(ctx);

            // 2. 为每个批次 backward
            for (LotBatch b : batches) {
                BigDecimal hoursCeil = rateService.computeProcessHoursCeil(b.getQty());
                int h = hoursCeil.intValue();


                int workCenterId = ShiftUtil.getLineByItem(b.getItemId()); // 每个顶层件可能不同
                calendar.registerDailyTemplate(workCenterId);

                LocalDateTime due = TimeAlignUtil.ceilToHour(b.getDueDate());
                LocalDateTime start = calendar.subtractWholeHours(workCenterId, due, h);
                    // 重新计算真实 end（可能早于 due）
                LocalDateTime end = calendar.addWholeHours(workCenterId, start, h);

                ScheduleTask childTask = new ScheduleTask(
                        nextId,
                        b.getItemId(),
                        b.getQty(),
                        hoursCeil,
                        start,
                        end,
                        new ArrayList<Integer>()
                );
                childTask.setWorkCenterId(workCenterId);
                childTasks.add(childTask);

                for (Integer pid : b.getParentQtyMap().keySet()) {
                    Set<Integer> set = parentToChildren.computeIfAbsent(pid, k -> new LinkedHashSet<Integer>());
                    set.add(nextId);
                }
                nextId++;
            }
        }
        return new LayerResult(childTasks, parentToChildren);
    }
}