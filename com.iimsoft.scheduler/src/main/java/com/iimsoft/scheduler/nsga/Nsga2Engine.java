package com.iimsoft.scheduler.nsga;

import com.iimsoft.scheduler.model.ScheduleTask;

import java.util.*;

/**
 * 核心 NSGA-II 引擎（简化版，不含精细缓存）：
 * 1. 初始化种群
 * 2. 评估
 * 3. Fast non-dominated sort + crowding
 * 4. 选择 + 交叉 + 变异 → offspring
 * 5. 合并父子 -> 新一代
 */
public class Nsga2Engine {

    private final Random random;
    private final int populationSize;
    private final int generations;

    private final List<ScheduleObjective> objectives;
    private final CrossoverOperator crossover;
    private final MutationOperator mutation;
    private final NonDominatedSorter sorter = new NonDominatedSorter();
    private final CrowdingDistance crowding = new CrowdingDistance();
    private final TournamentSelector selector;

    private final ScheduleDecoder decoder;
    private final List<ScheduleTask> baselineTasks;

    public Nsga2Engine(Random random,
                       int populationSize,
                       int generations,
                       List<ScheduleObjective> objectives,
                       CrossoverOperator crossover,
                       MutationOperator mutation,
                       TournamentSelector selector,
                       ScheduleDecoder decoder,
                       List<ScheduleTask> baselineTasks) {
        this.random = random;
        this.populationSize = populationSize;
        this.generations = generations;
        this.objectives = objectives;
        this.crossover = crossover;
        this.mutation = mutation;
        this.selector = selector;
        this.decoder = decoder;
        this.baselineTasks = baselineTasks;
    }

    public List<Chromosome> run() {
        List<Chromosome> population = initPopulation();

        evaluatePopulation(population);

        for (int gen = 0; gen < generations; gen++) {
            // 排序 + crowding
            assignRankAndCrowding(population);

            // 生成子代
            List<Chromosome> offspring = new ArrayList<Chromosome>();
            while (offspring.size() < populationSize) {
                Chromosome p1 = selector.select(population);
                Chromosome p2 = selector.select(population);
                Chromosome child = crossover.crossover(p1, p2);
                mutation.mutate(child);
                offspring.add(child);
            }
            evaluatePopulation(offspring);

            // 合并父+子
            List<Chromosome> merged = new ArrayList<Chromosome>();
            merged.addAll(population);
            merged.addAll(offspring);

            assignRankAndCrowding(merged);

            // 按 rank + crowding 选取下一代
            population = selectNextGeneration(merged);
            System.out.println("[NSGA-II] Generation " + gen + " completed. Pop=" + population.size());
        }

        assignRankAndCrowding(population);
        return population; // 包含多个 Pareto fronts
    }

    private void evaluatePopulation(List<Chromosome> pop) {
        for (Chromosome c : pop) {
            // 解码
            List<ScheduleTask> decoded = decoder.decode(c, baselineTasks);
            double[] vals = new double[objectives.size()];
            for (int i = 0; i < objectives.size(); i++) {
                vals[i] = objectives.get(i).evaluate(decoded);
            }
            c.setObjectives(vals);
        }
    }

    private void assignRankAndCrowding(List<Chromosome> pop) {
        List<List<Chromosome>> fronts = sorter.sort(pop);
        for (List<Chromosome> front : fronts) {
            crowding.assign(front);
        }
    }

    private List<Chromosome> selectNextGeneration(List<Chromosome> merged) {
        // 按 rank 升序，组内按 crowding 降序
        List<List<Chromosome>> fronts = new NonDominatedSorter().sort(merged);
        List<Chromosome> next = new ArrayList<Chromosome>(populationSize);
        for (List<Chromosome> f : fronts) {
            crowding.assign(f);
            if (next.size() + f.size() <= populationSize) {
                next.addAll(f);
            } else {
                // 需要裁剪
                Collections.sort(f, new Comparator<Chromosome>() {
                    public int compare(Chromosome a, Chromosome b) {
                        return Double.compare(b.getCrowdingDistance(), a.getCrowdingDistance());
                    }
                });
                int remain = populationSize - next.size();
                next.addAll(f.subList(0, remain));
                break;
            }
        }
        return next;
    }

    private List<Chromosome> initPopulation() {
        // 基于 baselineTasks 构建：每个 WC 获取任务 ID 列表；打乱形成多个染色体
        Map<Integer, List<Integer>> wcTaskMap = new LinkedHashMap<Integer, List<Integer>>();
        for (ScheduleTask t : baselineTasks) {
            wcTaskMap.computeIfAbsent(t.getWorkCenterId(), k -> new ArrayList<Integer>())
                    .add(t.getTaskId());
        }
        List<Chromosome> pop = new ArrayList<Chromosome>();
        for (int i = 0; i < populationSize; i++) {
            Map<Integer, int[]> seqs = new LinkedHashMap<Integer, int[]>();
            for (Map.Entry<Integer, List<Integer>> e : wcTaskMap.entrySet()) {
                List<Integer> ids = new ArrayList<Integer>(e.getValue());
                Collections.shuffle(ids, random);
                int[] arr = new int[ids.size()];
                for (int k = 0; k < ids.size(); k++) arr[k] = ids.get(k);
                seqs.put(e.getKey(), arr);
            }
            pop.add(new Chromosome(seqs));
        }
        return pop;
    }
}