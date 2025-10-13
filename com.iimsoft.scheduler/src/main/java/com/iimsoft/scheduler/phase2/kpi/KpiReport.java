package com.iimsoft.scheduler.phase2.kpi;

import java.util.*;

public class KpiReport {

    private final Date generatedAt = new Date();
    private final List<KpiSection> sections = new ArrayList<KpiSection>();
    private final Map<String, KpiValue> index = new LinkedHashMap<String, KpiValue>(); // name -> value

    public void addSection(KpiSection s) {
        sections.add(s);
        for (KpiValue v : s.getValues()) {
            index.put(v.getName(), v);
        }
    }

    public List<KpiSection> getSections() {
        return Collections.unmodifiableList(sections);
    }

    public KpiValue get(String name) {
        return index.get(name);
    }

    public Date getGeneratedAt() { return generatedAt; }

    public Map<String, KpiValue> asMap() {
        return Collections.unmodifiableMap(index);
    }
}