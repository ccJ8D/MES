package com.iimsoft.scheduler.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 顶层（总成）每日需求输入模型（来自 MRP / 外部接口）。
 * Phase1：只处理顶层，不展开 BOM。
 */
public class DailyDemand {
    private final int itemId;
    private final LocalDate day;
    private final BigDecimal quantity;

    public DailyDemand(int itemId, LocalDate day, BigDecimal quantity) {
        this.itemId = itemId;
        this.day = day;
        this.quantity = quantity;
    }

    public int getItemId() { return itemId; }
    public LocalDate getDay() { return day; }
    public BigDecimal getQuantity() { return quantity; }

    @Override
    public String toString() {
        return "DailyDemand{" +
                "itemId=" + itemId +
                ", day=" + day +
                ", quantity=" + quantity +
                '}';
    }
}