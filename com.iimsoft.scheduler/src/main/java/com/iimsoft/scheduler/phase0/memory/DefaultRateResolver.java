package com.iimsoft.scheduler.phase0.memory;

import com.iimsoft.scheduler.phase0.RateResolver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class DefaultRateResolver implements RateResolver {

    private final Map<Integer, BigDecimal> itemRates;

    public DefaultRateResolver(Map<Integer, BigDecimal> itemRates) {
        this.itemRates = itemRates;
    }

    @Override
    public BigDecimal getRateForItem(int itemId) {
        return itemRates.get(itemId);
    }

    @Override
    public BigDecimal computeProcessHoursCeil(BigDecimal quantity, BigDecimal rate) {
        if (quantity == null || quantity.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal raw = quantity.divide(rate, 10, RoundingMode.HALF_UP);
        return raw.setScale(0, RoundingMode.UP);
    }
}