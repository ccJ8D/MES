package com.iimsoft.scheduler.nsga;

import java.util.*;

public class TournamentSelector {

    private final Random random;
    private final int k; // tournament size

    public TournamentSelector(Random random, int k) {
        this.random = random;
        this.k = k <= 1 ? 2 : k;
    }

    public Chromosome select(List<Chromosome> population) {
        Chromosome best = null;
        for (int i = 0; i < k; i++) {
            Chromosome c = population.get(random.nextInt(population.size()));
            if (best == null) {
                best = c;
            } else {
                if (isBetter(c, best)) {
                    best = c;
                }
            }
        }
        return best;
    }

    private boolean isBetter(Chromosome a, Chromosome b) {
        if (a.getRank() < b.getRank()) return true;
        if (a.getRank() > b.getRank()) return false;
        return a.getCrowdingDistance() > b.getCrowdingDistance();
    }
}