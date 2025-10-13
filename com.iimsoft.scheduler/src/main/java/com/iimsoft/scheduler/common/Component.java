package com.iimsoft.scheduler.common;

import lombok.Data;

import java.math.BigDecimal;

/**
 * BOM 中父子关系：parentItemId --(usageQty)--> childItemId
 * 仅 Phase2：不含生效日期/替代料；后续可扩展。
 */
@Data
public class Component {
    private final int parentItemId;
    private final int childItemId;
    private final BigDecimal usage; // 父1件需要子 usage 件
    private final BigDecimal bufferHours; // 父→子提前小时（可为 null 视为 0）

    public Component(int parentItemId, int childItemId, BigDecimal usage, BigDecimal bufferHours) {
        this.parentItemId = parentItemId;
        this.childItemId = childItemId;
        this.usage = usage;
        this.bufferHours = bufferHours;
    }


    @Override
    public String toString() {
        return "Component{" +
                "parent=" + parentItemId +
                ", child=" + childItemId +
                ", usage=" + usage +
                '}';
    }
}