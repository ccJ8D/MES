package com.iimsoft.scheduler.kpi;

import java.util.Iterator;
import java.util.Map;

public final class KpiJsonExporter {

    private KpiJsonExporter(){}

    public static String toJson(KpiReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"generatedAt\":\"").append(report.getGeneratedAt()).append("\",");
        sb.append("\"sections\":[");
        for (int i=0;i<report.getSections().size();i++) {
            KpiSection sec = report.getSections().get(i);
            if (i>0) sb.append(",");
            sb.append("{\"name\":\"").append(sec.getName()).append("\",");
            sb.append("\"values\":[");
            for (int j=0;j<sec.getValues().size();j++) {
                KpiValue v = sec.getValues().get(j);
                if (j>0) sb.append(",");
                sb.append("{\"name\":\"").append(v.getName()).append("\",")
                  .append("\"value\":\"").append(escape(v.getValue())).append("\"");
                if (v.getUnit()!=null) {
                    sb.append(",\"unit\":\"").append(v.getUnit()).append("\"");
                }
                if (v.getNote()!=null) {
                    sb.append(",\"note\":\"").append(escape(v.getNote())).append("\"");
                }
                sb.append("}");
            }
            sb.append("]}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s==null) return "";
        return s.replace("\"","\\\"");
    }
}