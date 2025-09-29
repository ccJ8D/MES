package com.iimsoft.scheduler.v5.optimization;


// 15. NSGA-II优化器 - optimization/NSGA2Optimizer.java


import java.util.*;
import com.iimsoft.scheduler.v5.model.*;

public class NSGA2Optimizer {
    private final ObjectiveCalculator calculator;
    private final List<ShopOrder> orders;
    private final int populationSize;
    private final int maxGenerations;
    private final double crossoverRate;
    private final double mutationRate;
    private final Random random = new Random();
    
    public NSGA2Optimizer(ObjectiveCalculator calculator, List<ShopOrder> orders,
                         int populationSize, int maxGenerations, 
                         double crossoverRate, double mutationRate) {
        this.calculator = calculator;
        this.orders = orders;
        this.populationSize = populationSize;
        this.maxGenerations = maxGenerations;
        this.crossoverRate = crossoverRate;
        this.mutationRate = mutationRate;
    }
    
    public List<Chromosome> optimize() {
        // 1. 初始化种群
        List<Chromosome> population = initializePopulation();
        evaluatePopulation(population);
        
        // 2. 进化循环
        for (int generation = 0; generation < maxGenerations; generation++) {
            // 2.1 生成子代
            List<Chromosome> offspring = new ArrayList<>();
            while (offspring.size() < populationSize) {
                Chromosome parent1 = tournamentSelection(population);
                Chromosome parent2 = tournamentSelection(population);
                
                List<Chromosome> children = crossover(parent1, parent2);
                mutate(children.get(0));
                mutate(children.get(1));
                
                offspring.addAll(children);
            }
            
            // 2.2 评估子代
            evaluatePopulation(offspring);
            
            // 2.3 合并种群并选择下一代
            List<Chromosome> combined = new ArrayList<>(population);
            combined.addAll(offspring);
            
            List<List<Chromosome>> fronts = fastNonDominatedSort(combined);
            population = new ArrayList<>();
            
            for (List<Chromosome> front : fronts) {
                if (population.size() + front.size() <= populationSize) {
                    population.addAll(front);
                } else {
                    calculateCrowdingDistance(front);
                    front.sort((a, b) -> Double.compare(b.getCrowdingDistance(), 
                                                      a.getCrowdingDistance()));
                    int remaining = populationSize - population.size();
                    population.addAll(front.subList(0, remaining));
                    break;
                }
            }
            
            System.out.println("Generation " + generation + 
                ": Front size = " + fronts.get(0).size());
        }
        
        // 3. 返回Pareto前沿
        List<Chromosome> firstFront = fastNonDominatedSort(population).get(0);
        calculateCrowdingDistance(firstFront);
        return firstFront;
    }
    
    private List<Chromosome> initializePopulation() {
        List<Chromosome> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            List<ShopOrder> shuffled = new ArrayList<>(orders);
            Collections.shuffle(shuffled);
            population.add(new Chromosome(shuffled));
        }
        return population;
    }
    
    private void evaluatePopulation(List<Chromosome> population) {
        for (Chromosome chromosome : population) {
            calculator.evaluate(chromosome);
        }
    }
    
    private Chromosome tournamentSelection(List<Chromosome> population) {
        Chromosome best = population.get(random.nextInt(population.size()));
        for (int i = 1; i < 2; i++) { // 锦标赛规模=2
            Chromosome candidate = population.get(random.nextInt(population.size()));
            if (dominates(candidate, best)) {
                best = candidate;
            } else if (!dominates(best, candidate) && 
                      candidate.getCrowdingDistance() > best.getCrowdingDistance()) {
                best = candidate;
            }
        }
        return best.copy();
    }
    
    private List<Chromosome> crossover(Chromosome parent1, Chromosome parent2) {
        if (random.nextDouble() > crossoverRate) {
            return Arrays.asList(parent1.copy(), parent2.copy());
        }
        
        // Order Crossover (OX)
        int size = parent1.getSequence().size();
        int start = random.nextInt(size);
        int end = random.nextInt(size);
        
        if (start > end) {
            int temp = start;
            start = end;
            end = temp;
        }
        
        List<ShopOrder> child1 = new ArrayList<>(Collections.nCopies(size, null));
        List<ShopOrder> child2 = new ArrayList<>(Collections.nCopies(size, null));
        
        // 复制中间段
        for (int i = start; i <= end; i++) {
            child1.set(i, parent1.getSequence().get(i));
            child2.set(i, parent2.getSequence().get(i));
        }
        
        // 填充剩余部分
        fillRemaining(child1, parent2.getSequence(), end);
        fillRemaining(child2, parent1.getSequence(), end);
        
        return Arrays.asList(new Chromosome(child1), new Chromosome(child2));
    }
    
    private void fillRemaining(List<ShopOrder> child, List<ShopOrder> parent, int end) {
        int size = child.size();
        int index = (end + 1) % size;
        
        for (ShopOrder order : parent) {
            if (!child.contains(order)) {
                child.set(index, order);
                index = (index + 1) % size;
            }
        }
    }
    
    private void mutate(Chromosome chromosome) {
        if (random.nextDouble() > mutationRate) return;
        
        List<ShopOrder> sequence = chromosome.getSequence();
        int i = random.nextInt(sequence.size());
        int j = random.nextInt(sequence.size());
        Collections.swap(sequence, i, j);
    }
    
    private List<List<Chromosome>> fastNonDominatedSort(List<Chromosome> population) {
        List<List<Chromosome>> fronts = new ArrayList<>();
        Map<Chromosome, List<Chromosome>> dominatedSolutions = new HashMap<>();
        Map<Chromosome, Integer> dominationCount = new HashMap<>();
        
        List<Chromosome> firstFront = new ArrayList<>();
        
        for (Chromosome p : population) {
            dominatedSolutions.put(p, new ArrayList<>());
            dominationCount.put(p, 0);
            
            for (Chromosome q : population) {
                if (dominates(p, q)) {
                    dominatedSolutions.get(p).add(q);
                } else if (dominates(q, p)) {
                    dominationCount.put(p, dominationCount.get(p) + 1);
                }
            }
            
            if (dominationCount.get(p) == 0) {
                p.setRank(0);
                firstFront.add(p);
            }
        }
        
        fronts.add(firstFront);
        int i = 0;
        
        while (!fronts.get(i).isEmpty()) {
            List<Chromosome> nextFront = new ArrayList<>();
            
            for (Chromosome p : fronts.get(i)) {
                for (Chromosome q : dominatedSolutions.get(p)) {
                    dominationCount.put(q, dominationCount.get(q) - 1);
                    if (dominationCount.get(q) == 0) {
                        q.setRank(i + 1);
                        nextFront.add(q);
                    }
                }
            }
            
            i++;
            fronts.add(nextFront);
        }
        
        return fronts;
    }
    
    private void calculateCrowdingDistance(List<Chromosome> front) {
        int numObjectives = front.get(0).getObjectives().length;
        
        for (Chromosome solution : front) {
            solution.setCrowdingDistance(0);
        }
        
        for (int i = 0; i < numObjectives; i++) {
            final int objectiveIndex = i;
            front.sort(Comparator.comparingDouble(c -> c.getObjectives()[objectiveIndex]));
            
            front.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
            front.get(front.size() - 1).setCrowdingDistance(Double.POSITIVE_INFINITY);
            
            double minObjective = front.get(0).getObjectives()[objectiveIndex];
            double maxObjective = front.get(front.size() - 1).getObjectives()[objectiveIndex];
            
            if (maxObjective - minObjective == 0) continue;
            
            for (int j = 1; j < front.size() - 1; j++) {
                double distance = front.get(j).getCrowdingDistance();
                distance += (front.get(j + 1).getObjectives()[objectiveIndex] - 
                           front.get(j - 1).getObjectives()[objectiveIndex]) / 
                           (maxObjective - minObjective);
                front.get(j).setCrowdingDistance(distance);
            }
        }
    }
    
    private boolean dominates(Chromosome a, Chromosome b) {
        boolean betterInOne = false;
        double[] objectivesA = a.getObjectives();
        double[] objectivesB = b.getObjectives();
        
        for (int i = 0; i < objectivesA.length; i++) {
            if (objectivesA[i] > objectivesB[i]) {
                return false; // 目标都是最小化，所以a不能支配b
            } else if (objectivesA[i] < objectivesB[i]) {
                betterInOne = true;
            }
        }
        
        return betterInOne;
    }
}