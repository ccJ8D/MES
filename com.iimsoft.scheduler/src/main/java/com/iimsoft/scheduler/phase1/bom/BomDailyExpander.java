package com.iimsoft.scheduler.phase1.bom;

import com.iimsoft.scheduler.phase1.model.ChildDailyDemandTable;
import com.iimsoft.scheduler.common.Component;
import com.iimsoft.scheduler.common.DailyDemand;
import com.iimsofttech.exceptions.IimsofttechException;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 根据“顶层日需求” + “多级 BOM” 计算所有子件的日需求。
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


    public ChildDailyDemandTable expand(List<DailyDemand> topLevelDemands) {
        ChildDailyDemandTable table = new ChildDailyDemandTable();

        Map<Integer, Map<LocalDate, BigDecimal>> aggregateTop = aggregate(topLevelDemands);

        for (Map.Entry<Integer, Map<LocalDate, BigDecimal>> itemEntry : aggregateTop.entrySet()) {
            int topItem = itemEntry.getKey();
            for (Map.Entry<LocalDate, BigDecimal> dayEntry : itemEntry.getValue().entrySet()) {
                LocalDate day = dayEntry.getKey();
                BigDecimal qty = dayEntry.getValue();
                expandRecursive(topItem, day, qty, 0, table, new ArrayDeque<>());
            }
        }
        return table;
    }

    private Map<Integer, Map<LocalDate, BigDecimal>> aggregate(List<DailyDemand> list) {
        Map<Integer, Map<LocalDate, BigDecimal>> agg = new HashMap<>();
        for (DailyDemand d : list) {
            if (d.getQuantity() == null || d.getQuantity().signum() <= 0) continue;
            agg.computeIfAbsent(d.getItemId(), k -> new HashMap<>()).merge(d.getDay(), d.getQuantity(), BigDecimal::add);
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
            throw new IimsofttechException("物料深度超过限制: " + maxDepth + ", path: " + stack + " -> " + parentItem);
        }
        if (stack.contains(parentItem)) {
            throw new IimsofttechException("物料结构循环: " + stack + " -> " + parentItem);
        }

        List<Component> children = bomProvider.getComponentsOf(parentItem);
        if (children.isEmpty()) {
            return; // 叶子，不继续
        }

        stack.push(parentItem);
        for (Component c : children) {
            BigDecimal childQty = parentQtyForDay.multiply(c.getUsage());

            sink.add(c.getChildItemId(), day, childQty);
            expandRecursive(c.getChildItemId(), day, childQty, depth + 1, sink, stack);
        }
        stack.pop();
    }
}