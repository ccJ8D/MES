package com.iimsoft.scheduler.model;

import java.math.BigDecimal;

/**
 * BOM 中父子关系：parentItemId --(usageQty)--> childItemId
 * 仅 Phase2：不含生效日期/替代料；后续可扩展。
 */
public class Component {
    private final int parentItemId;
    private final int childItemId;
    private final BigDecimal usage; // 父1件需要子 usage 件

    public Component(int parentItemId, int childItemId, BigDecimal usage) {
        this.parentItemId = parentItemId;
        this.childItemId = childItemId;
        this.usage = usage;
    }

    public int getParentItemId() { return parentItemId; }
    public int getChildItemId() { return childItemId; }
    public BigDecimal getUsage() { return usage; }

    @Override
    public String toString() {
        return "Component{" +
                "parent=" + parentItemId +
                ", child=" + childItemId +
                ", usage=" + usage +
                '}';
    }
}