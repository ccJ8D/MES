package com.iimsoft.scheduler.phase2.kpi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * KPI 分段：例如 global / resource / batches / slack / violations
 */
public class KpiSection {
    private final String name;
    private final List<KpiValue> values = new ArrayList<KpiValue>();

    public KpiSection(String name) {
        this.name = name;
    }

    public KpiSection add(KpiValue v) {
        values.add(v);
        return this;
    }

    public String getName() { return name; }
    public List<KpiValue> getValues() {
        return Collections.unmodifiableList(values);
    }
}