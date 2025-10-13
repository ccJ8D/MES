package com.iimsoft.scheduler.phase1.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *  - 简单：固定产率（pieces per hour）
 *  - 后续可接入 standard_rate 表 / setup 时间 / 并行机效率
 *  - 产率单位：件/小时
 *  - 获取物料标准产量速率的服务
 */
public class RateService {

    private final BigDecimal piecesPerHour;

    public RateService(BigDecimal piecesPerHour) {
        if (piecesPerHour == null || piecesPerHour.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("piecesPerHour must > 0");
        }
        this.piecesPerHour = piecesPerHour;
    }

    public BigDecimal computeProcessHours(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        // hours = qty / rate
        return quantity.divide(piecesPerHour, 2, RoundingMode.UP);
    }

    /**
     * 新增：向上取整到整数小时。
     */
    public BigDecimal computeProcessHoursCeil(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal raw = quantity.divide(piecesPerHour, 10, RoundingMode.HALF_UP);
        return raw.setScale(0, RoundingMode.UP);
    }

    public BigDecimal getPiecesPerHour() {
        return piecesPerHour;
    }
}