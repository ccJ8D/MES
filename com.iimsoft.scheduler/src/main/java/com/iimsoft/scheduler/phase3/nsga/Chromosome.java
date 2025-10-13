package com.iimsoft.scheduler.phase3.nsga;

import java.util.*;

/**
 * 染色体：每个工作中心一段 ID 序列。
 * 为防止跨 WC 交换，结构是 Map<wcId, int[] taskOrder>
 * 保持所有任务 ID 完整覆盖且不重复。
 */
public class Chromosome implements Cloneable {

    private final Map<Integer, int[]> wcSequences; // 不可变引用
    private double crowdingDistance;
    private int rank; // Pareto front rank
    private double[] objectives; // 评估结果缓存

    public Chromosome(Map<Integer, int[]> wcSequences) {
        this.wcSequences = wcSequences;
    }

    public Map<Integer, int[]> getWcSequences() { return wcSequences; }

    public double[] getObjectives() { return objectives; }
    public void setObjectives(double[] objectives) { this.objectives = objectives; }

    public double getCrowdingDistance() { return crowdingDistance; }
    public void setCrowdingDistance(double cd) { this.crowdingDistance = cd; }

    public int getRank() { return rank; }
    public void setRank(int r) { this.rank = r; }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public Chromosome clone() {
        Map<Integer, int[]> copy = new LinkedHashMap<Integer, int[]>();
        for (Map.Entry<Integer, int[]> e : wcSequences.entrySet()) {
            int[] arr = e.getValue();
            int[] arrCopy = new int[arr.length];
            System.arraycopy(arr, 0, arrCopy, 0, arr.length);
            copy.put(e.getKey(), arrCopy);
        }
        Chromosome c = new Chromosome(copy);
        if (this.objectives != null) {
            c.objectives = this.objectives.clone();
        }
        c.rank = this.rank;
        c.crowdingDistance = this.crowdingDistance;
        return c;
    }

    @Override
    public String toString() {
        return "Chromosome{rank=" + rank +
                ", crowd=" + crowdingDistance +
                ", obj=" + (objectives==null? "null" : Arrays.toString(objectives)) + "}";
    }
}