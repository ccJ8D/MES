package com.iimsoft.scheduler.phase2.kpi;

import java.math.BigDecimal;
import java.util.*;

/**
 * 比较两个 KPI 报告，输出差值（仅对数值型可解析字段）。
 */
public final class KpiDiffUtil {

    private KpiDiffUtil(){}

    public static Map<String, String> diff(KpiReport baseline, KpiReport current) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        Set<String> keys = new LinkedHashSet<String>();
        keys.addAll(baseline.asMap().keySet());
        keys.addAll(current.asMap().keySet());

        for (String k : keys) {
            KpiValue vb = baseline.get(k);
            KpiValue vc = current.get(k);
            String before = vb == null ? "NA" : vb.getValue();
            String after  = vc == null ? "NA" : vc.getValue();
            BigDecimal bd = parseDecimal(before);
            BigDecimal ad = parseDecimal(after);
            if (bd != null && ad != null) {
                BigDecimal diff = ad.subtract(bd);
                String sign = diff.signum() > 0 ? "+" : "";
                result.put(k, before + " -> " + after + " (" + sign + diff + ")");
            } else {
                result.put(k, before + " -> " + after);
            }
        }
        return result;
    }

    private static BigDecimal parseDecimal(String val) {
        try {
            return new BigDecimal(val);
        } catch (Exception e) {
            return null;
        }
    }
}