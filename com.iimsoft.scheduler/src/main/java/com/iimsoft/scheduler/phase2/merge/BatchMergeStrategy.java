package com.iimsoft.scheduler.phase2.merge;

import com.iimsoft.scheduler.common.ScheduleTask;

import java.util.List;

/**
 * 定义批次合并策略：
 *   输入：已经完成资源序列化（无同资源重叠）的任务集合
 *   输出：可以合并的“分组”列表（每个分组是一批需要合并成一个的新任务）
 *
 * 约定：
 *   - 只返回 size >= 2 的分组；单元素不必返回
 *   - 分组内任务均属于同一工作中心
 *   - 调用方负责真正执行合并（数量相加、工时重算、父映射合并）
 */
public interface BatchMergeStrategy {

    /**
     * 计算可合并分组。
     * @param tasks 全部任务（可包含多个工作中心）
     * @return 分组列表
     */
    List<List<ScheduleTask>> findMergeGroups(List<ScheduleTask> tasks);
}