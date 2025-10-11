package com.iimsoft.scheduler.lot;


import com.iimsoft.scheduler.model.LevelContribution;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class LotSplitContext {
    private final int itemId;
    private final BigDecimal totalQty;
    private final LocalDateTime earliestRequired;
    private final LocalDateTime latestRequired;
    private final int level;
    private final List<LevelContribution> contributions;

    public LotSplitContext(int itemId,
                           BigDecimal totalQty,
                           LocalDateTime earliestRequired,
                           LocalDateTime latestRequired,
                           int level,
                           List<LevelContribution> contributions) {
        this.itemId = itemId;
        this.totalQty = totalQty;
        this.earliestRequired = earliestRequired;
        this.latestRequired = latestRequired;
        this.level = level;
        this.contributions = contributions;
    }

    public int getItemId() { return itemId; }
    public BigDecimal getTotalQty() { return totalQty; }
    public LocalDateTime getEarliestRequired() { return earliestRequired; }
    public LocalDateTime getLatestRequired() { return latestRequired; }
    public int getLevel() { return level; }
    public List<LevelContribution> getContributions() { return contributions; }
}