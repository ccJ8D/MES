package com.iimsoft.scheduler.phase1.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 保存多级展开后的“子件日需求”结果。
 * 结构：
 *   itemId -> ( LocalDate -> quantity )
 */
public class ChildDailyDemandTable {

    private final Map<Integer, Map<LocalDate, BigDecimal>> table = new HashMap<>();

    public void add(int itemId, LocalDate day, BigDecimal qty) {
        if (qty == null) return;
        if (qty.signum() <= 0) return;
        table.computeIfAbsent(itemId, k -> new HashMap<>())
                .merge(day, qty, BigDecimal::add);
    }

    public Map<Integer, Map<LocalDate, BigDecimal>> asMap() {
        // 返回浅拷贝（只读包装可视需求）
        Map<Integer, Map<LocalDate, BigDecimal>> copy = new HashMap<>();
        for (Map.Entry<Integer, Map<LocalDate, BigDecimal>> e  : table.entrySet()) {
            copy.put(e.getKey(), Collections.unmodifiableMap(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    public Map<LocalDate, BigDecimal> getDaysForItem(int itemId) {
        return table.getOrDefault(itemId, new HashMap<>());
    }

    public Set<Integer> getAllItemIds() {
        return Collections.unmodifiableSet(table.keySet());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ChildDailyDemandTable{\n");
        table.forEach((item, map) -> {
            sb.append("  item ").append(item).append(":\n");
            map.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(en -> sb.append("    ").append(en.getKey()).append(" -> ").append(en.getValue()).append("\n"));
        });
        sb.append("}");
        return sb.toString();
    }
}