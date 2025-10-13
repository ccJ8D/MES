package com.iimsoft.scheduler.phase0;

import java.math.BigDecimal;

/**
 * 可选：支持物料级别速率 (pieces/hour)。
 * 若返回 null 表示使用默认全局速率。
 */
public interface RateResolver {
    BigDecimal getRateForItem(int itemId);

    BigDecimal computeProcessHoursCeil(BigDecimal qty, BigDecimal rate);
}