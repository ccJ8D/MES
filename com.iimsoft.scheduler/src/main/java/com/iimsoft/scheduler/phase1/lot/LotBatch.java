package com.iimsoft.scheduler.phase1.lot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class LotBatch {
    private final int batchIndex;
    private final int itemId;
    private final BigDecimal qty;
    private final LocalDateTime dueDate;
    private final Map<Integer, BigDecimal> parentQtyMap; // parentTaskId -> allocated qty

    public LotBatch(int batchIndex,
                    int itemId,
                    BigDecimal qty,
                    LocalDateTime dueDate,
                    Map<Integer, BigDecimal> parentQtyMap) {
        this.batchIndex = batchIndex;
        this.itemId = itemId;
        this.qty = qty;
        this.dueDate = dueDate;
        this.parentQtyMap = parentQtyMap;
    }

    public int getBatchIndex() { return batchIndex; }
    public int getItemId() { return itemId; }
    public BigDecimal getQty() { return qty; }
    public LocalDateTime getDueDate() { return dueDate; }
    public Map<Integer, BigDecimal> getParentQtyMap() { return parentQtyMap; }
}