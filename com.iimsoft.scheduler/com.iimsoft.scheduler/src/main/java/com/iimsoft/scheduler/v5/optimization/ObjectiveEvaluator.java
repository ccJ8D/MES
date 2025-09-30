package com.iimsoft.scheduler.v5.optimization;

import com.iimsoft.scheduler.v5.model.Chromosome;

/**
 * Interface for objective function evaluation in optimization algorithms.
 * This interface allows different implementations for database-backed and in-memory
 * objective calculations while maintaining the same API for NSGA-II optimizer.
 */
public interface ObjectiveEvaluator {
    
    /**
     * Evaluates the objectives for a given chromosome (solution).
     * 
     * @param chromosome The chromosome representing a shop order sequence
     * @return Array of objective values [makespan, totalLateness, inventoryFluctuation]
     */
    double[] evaluate(Chromosome chromosome);
}