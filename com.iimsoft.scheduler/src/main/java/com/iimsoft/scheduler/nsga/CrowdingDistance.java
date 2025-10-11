package com.iimsoft.scheduler.nsga;

import java.util.*;

/**
 * 计算拥挤距离
 */
public class CrowdingDistance {

    public void assign(List<Chromosome> front) {
        int l = front.size();
        if (l == 0) return;
        int m = front.get(0).getObjectives().length;
        for (Chromosome c : front) {
            c.setCrowdingDistance(0);
        }
        for (int obj = 0; obj < m; obj++) {
            final int idx = obj;
            front.sort(new Comparator<Chromosome>() {
                public int compare(Chromosome o1, Chromosome o2) {
                    return Double.compare(o1.getObjectives()[idx], o2.getObjectives()[idx]);
                }
            });
            front.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
            front.get(l - 1).setCrowdingDistance(Double.POSITIVE_INFINITY);
            double min = front.get(0).getObjectives()[obj];
            double max = front.get(l - 1).getObjectives()[obj];
            double denom = max - min;
            if (denom == 0) denom = 1.0;
            for (int i = 1; i < l - 1; i++) {
                double prev = front.get(i - 1).getObjectives()[obj];
                double next = front.get(i + 1).getObjectives()[obj];
                double dist = (next - prev) / denom;
                double cur = front.get(i).getCrowdingDistance();
                front.get(i).setCrowdingDistance(cur + dist);
            }
        }
    }
}