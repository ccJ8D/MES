package com.iimsoft.scheduler.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Phase3 NEW:
 * 对应“某个父任务”对“某个子件”在 requiredBy 之前需要的数量。
 * requiredBy = parentTask.start - buffer(可选)
 */
public class ChildDemandContribution {
    private final int childItemId;
    private final BigDecimal quantity;
    private final LocalDateTime requiredBy;
    private final int parentTaskId;
    private final int bomLevel;

    public ChildDemandContribution(int childItemId,
                                   BigDecimal quantity,
                                   LocalDateTime requiredBy,
                                   int parentTaskId,
                                   int bomLevel) {
        this.childItemId = childItemId;
        this.quantity = quantity;
        this.requiredBy = requiredBy;
        this.parentTaskId = parentTaskId;
        this.bomLevel = bomLevel;
    }

    public int getChildItemId() { return childItemId; }
    public BigDecimal getQuantity() { return quantity; }
    public LocalDateTime getRequiredBy() { return requiredBy; }
    public int getParentTaskId() { return parentTaskId; }
    public int getBomLevel() { return bomLevel; }

    @Override
    public String toString() {
        return "Contribution{" +
                "child=" + childItemId +
                ", qty=" + quantity +
                ", requiredBy=" + requiredBy +
                ", parentTask=" + parentTaskId +
                ", level=" + bomLevel +
                '}';
    }
}