package com.iimsoft.scheduler.facade;

import com.iimsoft.scheduler.model.ScheduleTask;
import com.iimsoft.scheduler.nsga.*;
import com.iimsoft.scheduler.service.RateService;
import com.iimsoft.scheduler.shift.WorkCalendarService;

import java.util.*;

/**
 * 对外入口：执行多目标优化，返回最终种群（含 Pareto 前沿）。
 */
public class Phase5OptimizerFacade {

    public static class Config {
        public int populationSize = 40;
        public int generations = 30;
        public double crossoverRate = 0.9;
        public double mutationRate = 0.1;
        public int tournamentSize = 3;
        public boolean applyBatchMerge = false;
        public boolean applyTightening = false;
    }

    public static class Result {
        public final List<Chromosome> finalPopulation;
        public final List<Chromosome> firstFront;
        public Result(List<Chromosome> finalPopulation, List<Chromosome> firstFront) {
            this.finalPopulation = finalPopulation;
            this.firstFront = firstFront;
        }
    }

    private final WorkCalendarService calendar;
    private final RateService rateService;
    private final Config cfg;

    public Phase5OptimizerFacade(WorkCalendarService calendar,
                                 RateService rateService,
                                 Config cfg) {
        this.calendar = calendar;
        this.rateService = rateService;
        this.cfg = cfg;
    }

    public Result optimize(List<ScheduleTask> baselineTasks) {
        List<ScheduleObjective> objectives = new ArrayList<ScheduleObjective>();
        objectives.add(new MakespanObjective());
        objectives.add(new TotalSlackObjective());
        objectives.add(new ResourceLoadStdDevObjective());
        // 可以追加其它目标

        ScheduleDecoder decoder = new ScheduleDecoder(calendar, rateService,
                cfg.applyBatchMerge, cfg.applyTightening);

        Random rnd = new Random(42);

        Nsga2Engine engine = new Nsga2Engine(
                rnd,
                cfg.populationSize,
                cfg.generations,
                objectives,
                new CrossoverOperator(rnd, cfg.crossoverRate),
                new MutationOperator(rnd, cfg.mutationRate),
                new TournamentSelector(rnd, cfg.tournamentSize),
                decoder,
                baselineTasks
        );

        List<Chromosome> population = engine.run();

        // 抽取 rank=0 的前沿
        List<Chromosome> firstFront = new ArrayList<Chromosome>();
        for (Chromosome c : population) {
            if (c.getRank() == 0) firstFront.add(c);
        }

        return new Result(population, firstFront);
    }
}