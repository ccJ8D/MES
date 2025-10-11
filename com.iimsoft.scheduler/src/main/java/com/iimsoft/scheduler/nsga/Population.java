package com.iimsoft.scheduler.nsga;

import java.util.*;

public class Population {
    private final List<Individual> individuals = new ArrayList<Individual>();

    public void add(Individual ind) { individuals.add(ind); }
    public List<Individual> getIndividuals() { return individuals; }
    public int size() { return individuals.size(); }

    public Individual get(int idx) { return individuals.get(idx); }
}