package com.iimsoft.scheduler.util;

import java.time.LocalDateTime;

/**
 * 时间整点对齐工具：所有结果都向上取整到下一个整点（若已整点则保持）。
 * 用于：buffer 应用后、外部输入 dueDate 校正（若出现非整点）、子件 requiredBy 调整。
 */
public final class TimeAlignUtil {

    private TimeAlignUtil() {}

    /**
     * 向上取整到“整点”，秒与纳秒被清零。
     * 例如：13:00 -> 13:00; 13:00:01 -> 14:00; 13:15 -> 14:00
     */
    public static LocalDateTime ceilToHour(LocalDateTime t) {
        if (t == null) return null;
        if (t.getMinute() == 0 && t.getSecond() == 0 && t.getNano() == 0) return t;
        return t.plusHours(1).withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * 强制去掉分钟、秒、纳秒（向下清零）。仅在特殊需要保持不后移时使用。
     */
    public static LocalDateTime floorHour(LocalDateTime t) {
        if (t == null) return null;
        return t.withMinute(0).withSecond(0).withNano(0);
    }
}