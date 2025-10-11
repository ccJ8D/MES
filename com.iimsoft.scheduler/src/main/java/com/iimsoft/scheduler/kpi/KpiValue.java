package com.iimsoft.scheduler.kpi;

import java.math.BigDecimal;

/**
 * 单一指标条目。
 * 允许用 BigDecimal 或 Long / Double（统一转为字符串输出）。
 */
public class KpiValue {
    private final String name;
    private final String value;
    private final String unit;
    private final String note;

    public KpiValue(String name, String value, String unit, String note) {
        this.name = name;
        this.value = value;
        this.unit = unit;
        this.note = note;
    }

    public static KpiValue of(String name, Object val) {
        return new KpiValue(name, val == null ? "null" : String.valueOf(val), null, null);
    }

    public static KpiValue of(String name, Object val, String unit, String note) {
        return new KpiValue(name, val == null ? "null" : String.valueOf(val), unit, note);
    }

    public String getName() { return name; }
    public String getValue() { return value; }
    public String getUnit() { return unit; }
    public String getNote() { return note; }
}