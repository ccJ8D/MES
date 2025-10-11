package com.iimsoft.scheduler.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 层序算法中某一“直接层”父任务对一个子件的需求贡献记录。
 * parentTaskId 为该层已排父任务 ID。
 */
public class LevelContribution {
    private final int childItemId;
    private final BigDecimal quantity;
    private final LocalDateTime requiredBy; // earliest parent start - buffer 已计算完
    private final int parentTaskId;
    private final int level; // 父任务层级（顶层=0，子件=1，…）

    public LevelContribution(int childItemId,
                             BigDecimal quantity,
                             LocalDateTime requiredBy,
                             int parentTaskId,
                             int level) {
        this.childItemId = childItemId;
        this.quantity = quantity;
        this.requiredBy = requiredBy;
        this.parentTaskId = parentTaskId;
        this.level = level;
    }

    public int getChildItemId() { return childItemId; }
    public BigDecimal getQuantity() { return quantity; }
    public LocalDateTime getRequiredBy() { return requiredBy; }
    public int getParentTaskId() { return parentTaskId; }
    public int getLevel() { return level; }
}