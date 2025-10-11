package com.iimsoft.scheduler.bom;

import com.iimsoft.scheduler.model.ChildDailyDemandTable;
import com.iimsoft.scheduler.model.Component;
import com.iimsoft.scheduler.model.DailyDemand;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Phase2 核心：根据“顶层日需求” + “多级 BOM” 计算所有子件的日需求。
 * 注意：
 *  - 不计算子件的 start/end
 *  - 不做 backward / requiredBy 精炼
 *  - 不切批
 *  - 不区分同一日内多个顶层任务（直接按日聚合）
 */
public class BomDailyExpander {

    private final BomProvider bomProvider;
    private final int maxDepth; // 防止异常循环（可配置）

    public BomDailyExpander(BomProvider bomProvider, int maxDepth) {
        this.bomProvider = bomProvider;
        this.maxDepth = maxDepth <= 0 ? 10 : maxDepth;
    }

    /**
     * @param topLevelDemands 顶层日需求（Phase1 输入）
     * @return 所有子件（不含顶层自身）日需求表
     */
    public ChildDailyDemandTable expand(List<DailyDemand> topLevelDemands) {
        ChildDailyDemandTable table = new ChildDailyDemandTable();

        // 将同一 itemId, day 聚合后再展开（减少递归次数）
        Map<Integer, Map<LocalDate, BigDecimal>> aggregateTop = aggregate(topLevelDemands);

        for (Map.Entry<Integer, Map<LocalDate, BigDecimal>> itemEntry : aggregateTop.entrySet()) {
            int topItem = itemEntry.getKey();
            for (Map.Entry<LocalDate, BigDecimal> dayEntry : itemEntry.getValue().entrySet()) {
                LocalDate day = dayEntry.getKey();
                BigDecimal qty = dayEntry.getValue();
                // 递归展开
                expandRecursive(topItem, day, qty, 0, table, new ArrayDeque<>());
            }
        }
        return table;
    }

    private Map<Integer, Map<LocalDate, BigDecimal>> aggregate(List<DailyDemand> list) {
        Map<Integer, Map<LocalDate, BigDecimal>> agg = new HashMap<>();
        for (DailyDemand d : list) {
            if (d.getQuantity() == null || d.getQuantity().signum() <= 0) continue;
            agg.computeIfAbsent(d.getItemId(), k -> new HashMap<>())
                    .merge(d.getDay(), d.getQuantity(), BigDecimal::add);
        }
        return agg;
    }

    private void expandRecursive(int parentItem,
                                 LocalDate day,
                                 BigDecimal parentQtyForDay,
                                 int depth,
                                 ChildDailyDemandTable sink,
                                 Deque<Integer> stack) {

        if (depth > maxDepth) {
            throw new IllegalStateException("BOM depth exceeds maxDepth=" + maxDepth + " at parent " + parentItem);
        }
        if (stack.contains(parentItem)) {
            throw new IllegalStateException("BOM cycle detected: " + stack + " -> " + parentItem);
        }

        List<Component> children = bomProvider.getComponentsOf(parentItem);
        if (children.isEmpty()) {
            return; // 叶子，不继续
        }

        stack.push(parentItem);
        for (Component c : children) {
            BigDecimal childQty = parentQtyForDay.multiply(c.getUsage());
            // 记录子件日需求（不包含顶层本身，所以这里直接添加）
            sink.add(c.getChildItemId(), day, childQty);

            // 继续向下展开
            expandRecursive(c.getChildItemId(), day, childQty, depth + 1, sink, stack);
        }
        stack.pop();
    }
}