package com.iimsoft.scheduler.phase3.nsga;

import java.util.*;

/**
 * 简单交换变异：对每个工作中心序列以 mutationRate 概率随机交换两个位置
 */
public class MutationOperator {

    private final Random random;
    private final double mutationRate;

    public MutationOperator(Random random, double mutationRate) {
        this.random = random;
        this.mutationRate = mutationRate;
    }

    public void mutate(Chromosome chrom) {
        for (Map.Entry<Integer, int[]> e : chrom.getWcSequences().entrySet()) {
            int[] seq = e.getValue();
            if (seq.length < 2) continue;
            if (random.nextDouble() <= mutationRate) {
                int i = random.nextInt(seq.length);
                int j = random.nextInt(seq.length);
                int tmp = seq[i];
                seq[i] = seq[j];
                seq[j] = tmp;
            }
        }
    }
}