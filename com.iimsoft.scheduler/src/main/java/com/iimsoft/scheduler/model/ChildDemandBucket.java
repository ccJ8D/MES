package com.iimsoft.scheduler.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Phase3 NEW:
 * 聚合同一子件的所有贡献，并维护统计指标（最大层级、最早/最晚 requiredBy、总量）。
 */
public class ChildDemandBucket {
    private final int itemId;
    private final List<ChildDemandContribution> contributions = new ArrayList<>();
    private BigDecimal totalQty = BigDecimal.ZERO;
    private LocalDateTime earliest;
    private LocalDateTime latest;
    private int maxBomLevel = 0;

    public ChildDemandBucket(int itemId) {
        this.itemId = itemId;
    }

    public void add(ChildDemandContribution c) {
        contributions.add(c);
        totalQty = totalQty.add(c.getQuantity());
        if (earliest == null || c.getRequiredBy().isBefore(earliest)) earliest = c.getRequiredBy();
        if (latest == null || c.getRequiredBy().isAfter(latest)) latest = c.getRequiredBy();
        if (c.getBomLevel() > maxBomLevel) maxBomLevel = c.getBomLevel();
    }

    public List<ChildDemandContribution> sortedByRequired() {
        contributions.sort(Comparator.comparing(ChildDemandContribution::getRequiredBy));
        return contributions;
    }

    public int getItemId() { return itemId; }
    public BigDecimal getTotalQty() { return totalQty; }
    public LocalDateTime getEarliest() { return earliest; }
    public LocalDateTime getLatest() { return latest; }
    public int getMaxBomLevel() { return maxBomLevel; }
    public int size() { return contributions.size(); }
}