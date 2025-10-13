package com.iimsoft.scheduler.phase0.db;

import com.iimsoft.mes.model.I_mom_item;
import com.iimsoft.mes.model.I_mom_standard_rate;
import com.iimsoft.scheduler.phase0.RateResolver;
import com.iimsoft.util.Services;
import com.iimsofttech.ad.dao.IQueryBL;
import com.iimsofttech.model.InterfaceWrapperHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBRateResolver implements RateResolver {
    private final Map<Integer, BigDecimal> itemRates = new HashMap<>();
    final IQueryBL queryBL = Services.get(IQueryBL.class);

    @Override
    public BigDecimal getRateForItem(int itemId) {
        I_mom_item item = InterfaceWrapperHelper.load(itemId, I_mom_item.class);
        List<I_mom_standard_rate> standardRates = queryBL.createQueryBuilder(I_mom_standard_rate.class)
                .addEqualsFilter(I_mom_standard_rate.COLUMNNAME_item_bo, itemId)
                .addEqualsFilter(I_mom_standard_rate.COLUMNNAME_router_bo, item.getbom_bo())
                .create().list();
        if (standardRates.isEmpty()) {
            return itemRates.computeIfAbsent(itemId, id -> BigDecimal.valueOf(100));
        } else {
            I_mom_standard_rate standardRate = standardRates.get(0);
            String uot = standardRate.getuot();
            BigDecimal time = standardRate.gettime();
            BigDecimal qty = standardRate.getQuantity();
            switch (uot) {
                case "H":
                    return itemRates.computeIfAbsent(itemId, id -> qty.divide(time, 10, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(60)));
                case "M":
                    // 每分钟，乘60转为每小时
                    return itemRates.computeIfAbsent(itemId, id -> qty.divide(time, 10, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(60)));
                case "S":
                    // 每秒，乘3600转为每小时
                    return itemRates.computeIfAbsent(itemId, id -> qty.divide(time, 10, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(3600)));
                default:
                    return itemRates.computeIfAbsent(itemId, id -> BigDecimal.valueOf(100));
            }
        }
    }

        @Override
        public BigDecimal computeProcessHoursCeil (BigDecimal quantity, BigDecimal rate){
            if (quantity == null || quantity.signum() <= 0) return BigDecimal.ZERO;
            BigDecimal raw = quantity.divide(rate, 10, RoundingMode.HALF_UP);
            return raw.setScale(0, RoundingMode.UP);
        }
    }
