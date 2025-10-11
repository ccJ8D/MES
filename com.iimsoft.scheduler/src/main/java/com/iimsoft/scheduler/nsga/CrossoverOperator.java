package com.iimsoft.scheduler.nsga;

import java.util.*;

/**
 * 多点顺序交叉（Order Crossover, OX）对每个工作中心序列独立执行。
 */
public class CrossoverOperator {

    private final Random random;
    private final double crossoverRate;

    public CrossoverOperator(Random random, double crossoverRate) {
        this.random = random;
        this.crossoverRate = crossoverRate;
    }

    public Chromosome crossover(Chromosome p1, Chromosome p2) {
        if (random.nextDouble() > crossoverRate) {
            return p1.clone();
        }
        Map<Integer, int[]> childSeq = new LinkedHashMap<Integer, int[]>();
        for (Integer wc : p1.getWcSequences().keySet()) {
            int[] a = p1.getWcSequences().get(wc);
            int[] b = p2.getWcSequences().get(wc);
            int[] c = oxSingle(a, b);
            childSeq.put(wc, c);
        }
        return new Chromosome(childSeq);
    }

    private int[] oxSingle(int[] a, int[] b) {
        int n = a.length;
        if (n <= 2) return a.clone();
        int left = random.nextInt(n);
        int right = random.nextInt(n);
        if (left > right) { int tmp = left; left = right; right = tmp; }

        int[] child = new int[n];
        Arrays.fill(child, -1);
        // Copy segment from a
        for (int i = left; i <= right; i++) {
            child[i] = a[i];
        }
        // Fill remaining from b in order
        Set<Integer> used = new HashSet<Integer>();
        for (int i = left; i <= right; i++) used.add(a[i]);
        int bi = 0;
        for (int i = 0; i < n; i++) {
            if (child[i] != -1) continue;
            while (used.contains(b[bi])) bi++;
            child[i] = b[bi];
            used.add(b[bi]);
            bi++;
        }
        return child;
    }
}