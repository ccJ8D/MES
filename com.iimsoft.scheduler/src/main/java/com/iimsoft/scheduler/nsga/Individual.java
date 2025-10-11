package com.iimsoft.scheduler.nsga;


/**
 * 简单封装：可以后续扩展添加缓存数据（如解码结果引用）
 */
public class Individual {
    private Chromosome chromosome;
    public Individual(Chromosome chromosome) {
        this.chromosome = chromosome;
    }
    public Chromosome getChromosome() { return chromosome; }
    public void setChromosome(Chromosome c) { this.chromosome = c; }
}