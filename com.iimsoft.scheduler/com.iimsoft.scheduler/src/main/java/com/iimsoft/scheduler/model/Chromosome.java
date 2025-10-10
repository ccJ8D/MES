package com.iimsoft.scheduler.model;


// 7. 染色体 - model/Chromosome.java
import java.util.*;

public class Chromosome {
    private List<ShopOrder> sequence;
    private double[] objectives;
    private int rank;
    private double crowdingDistance;
    
    public Chromosome(List<ShopOrder> sequence) {
        this.sequence = new ArrayList<>(sequence);
    }
    
    // Getters and Setters
    public List<ShopOrder> getSequence() { return sequence; }
    public double[] getObjectives() { return objectives; }
    public void setObjectives(double[] objectives) { this.objectives = objectives; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public double getCrowdingDistance() { return crowdingDistance; }
    public void setCrowdingDistance(double crowdingDistance) { 
        this.crowdingDistance = crowdingDistance; 
    }
    
    public Chromosome copy() {
        Chromosome copy = new Chromosome(new ArrayList<>(this.sequence));
        copy.setObjectives(this.objectives != null ? this.objectives.clone() : null);
        copy.setRank(this.rank);
        copy.setCrowdingDistance(this.crowdingDistance);
        return copy;
    }
}