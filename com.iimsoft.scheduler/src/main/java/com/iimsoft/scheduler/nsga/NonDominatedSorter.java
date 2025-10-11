package com.iimsoft.scheduler.nsga;

import java.util.*;

/**
 * NSGA-II Fast Non-dominated Sort
 */
public class NonDominatedSorter {

    public List<List<Chromosome>> sort(List<Chromosome> population) {
        List<List<Integer>> S = new ArrayList<List<Integer>>();
        int[] n = new int[population.size()];
        List<List<Chromosome>> fronts = new ArrayList<List<Chromosome>>();
        List<Integer> first = new ArrayList<Integer>();

        for (int p = 0; p < population.size(); p++) {
            S.add(new ArrayList<Integer>());
            n[p] = 0;
            for (int q = 0; q < population.size(); q++) {
                int flag = dominateCompare(population.get(p), population.get(q));
                if (flag == -1) {
                    S.get(p).add(q);
                } else if (flag == 1) {
                    n[p]++;
                }
            }
            if (n[p] == 0) {
                population.get(p).setRank(0);
                first.add(p);
            }
        }
        List<Chromosome> front0 = new ArrayList<Chromosome>();
        for (Integer idx : first) front0.add(population.get(idx));
        fronts.add(front0);

        int i = 0;
        while (i < fronts.size()) {
            List<Chromosome> nextFront = new ArrayList<Chromosome>();
            for (Chromosome pChrom : fronts.get(i)) {
                int pIndex = population.indexOf(pChrom);
                for (Integer q : S.get(pIndex)) {
                    n[q]--;
                    if (n[q] == 0) {
                        population.get(q).setRank(i + 1);
                        nextFront.add(population.get(q));
                    }
                }
            }
            if (nextFront.isEmpty()) break;
            fronts.add(nextFront);
            i++;
        }
        return fronts;
    }

    private int dominateCompare(Chromosome a, Chromosome b) {
        double[] ao = a.getObjectives();
        double[] bo = b.getObjectives();
        boolean betterInOne = false;
        boolean worseInOne = false;
        for (int i = 0; i < ao.length; i++) {
            if (ao[i] < bo[i]) {
                betterInOne = true;
            } else if (ao[i] > bo[i]) {
                worseInOne = true;
            }
        }
        if (betterInOne && !worseInOne) return -1; // a dominates b
        if (worseInOne && !betterInOne) return 1;  // b dominates a
        return 0; // non-dominated each other
    }
}