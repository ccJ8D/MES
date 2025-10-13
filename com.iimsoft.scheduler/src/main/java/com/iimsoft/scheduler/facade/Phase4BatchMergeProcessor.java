package com.iimsoft.scheduler.facade;

import com.iimsoft.scheduler.phase2.merge.BatchMergeStrategy;
import com.iimsoft.scheduler.phase2.merge.BatchMerger;
import com.iimsoft.scheduler.phase2.merge.MergeReport;
import com.iimsoft.scheduler.common.ScheduleTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 统一入口：
 *  1) 使用策略执行合并
 *  2) 返回新任务列表 + 报告
 *  3) 默认使用：SimpleExactWindowMergeStrategy
 *
 * 可扩展：
 *  - 传入多策略链（先 Exact 再 Tolerance）
 *  - 按工作中心拆分后再合并
 */
public class Phase4BatchMergeProcessor {

    private final BatchMerger batchMerger;
    private final BatchMergeStrategy strategy;

    public Phase4BatchMergeProcessor(BatchMerger batchMerger,
                                     BatchMergeStrategy strategy) {
        this.batchMerger = batchMerger;
        this.strategy = strategy;
    }

    public Result process(List<ScheduleTask> sequencedTasks) {
        // 深拷贝可选：这里直接引用（因为合并修改任务本身）
        List<ScheduleTask> copy = new ArrayList<ScheduleTask>(sequencedTasks);
        MergeReport report = batchMerger.merge(copy, strategy);
        // 返回 *更新后的* 任务列表：BatchMerger 内直接修改 master
        List<ScheduleTask> resultTasks = new ArrayList<ScheduleTask>();
        for (ScheduleTask t : copy) {
            // copy里被移除的在原逻辑中是通过 removed 过滤，这里直接再收集一次
            resultTasks.add(t);
        }
        return new Result(Collections.unmodifiableList(resultTasks), report);
    }

    public static class Result {
        private final List<ScheduleTask> tasks;
        private final MergeReport report;

        public Result(List<ScheduleTask> tasks, MergeReport report) {
            this.tasks = tasks;
            this.report = report;
        }

        public List<ScheduleTask> getTasks() { return tasks; }
        public MergeReport getReport() { return report; }
    }
}