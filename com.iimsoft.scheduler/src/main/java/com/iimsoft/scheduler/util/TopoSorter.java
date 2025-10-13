package com.iimsoft.scheduler.util;

import com.iimsoft.scheduler.common.ScheduleTask;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 通用拓扑排序（Kahn）：
 * - 输入：同一资源上的任务（含前置依赖可能指向其它资源 -> 非本资源的依赖视为外部完成，不影响本资源局部顺序，只在时间计算时考虑）
 * - 对本资源内部依赖建立图。
 * - 稳定性：同层入度为0时，按 (originalLevelGuess, originalStart, taskId) 排序。
 *   originalLevelGuess 简化：通过 earliest predecessor 层估算（这里用依赖数量替代，或外部可注入）。
 */
public class TopoSorter {

    public static List<ScheduleTask> topoOrderForWorkCenter(List<ScheduleTask> source,
                                                            Set<Integer> workCenterTaskIds) {

        // 1. 构建本资源内部依赖图
        Map<Integer, ScheduleTask> idMap = new HashMap<Integer, ScheduleTask>();
        for (ScheduleTask t : source) {
            idMap.put(t.getTaskId(), t);
        }

        Map<Integer, Integer> indeg = new HashMap<Integer, Integer>();
        Map<Integer, List<Integer>> adj = new HashMap<Integer, List<Integer>>();

        for (ScheduleTask t : source) {
            if (!workCenterTaskIds.contains(t.getTaskId())) continue;
            int in = 0;
            for (Integer pre : t.getPredecessors()) {
                if (workCenterTaskIds.contains(pre)) {
                    // 内部依赖才计算入度
                    in++;
                    List<Integer> list = adj.computeIfAbsent(pre, k -> new ArrayList<Integer>());
                    list.add(t.getTaskId());
                }
            }
            indeg.put(t.getTaskId(), in);
        }

        // 2. 初始化入度0队列
        PriorityQueue<ScheduleTask> pq = new PriorityQueue<ScheduleTask>(new Comparator<ScheduleTask>() {
            public int compare(ScheduleTask a, ScheduleTask b) {
                // originalStart 早者优先，再任务ID
                LocalDateTime as = a.getStart();
                LocalDateTime bs = b.getStart();
                int cmp = as.compareTo(bs);
                if (cmp != 0) return cmp;
                return Integer.compare(a.getTaskId(), b.getTaskId());
            }
        });

        for (ScheduleTask t : source) {
            if (workCenterTaskIds.contains(t.getTaskId())) {
                if (indeg.get(t.getTaskId()) != null && indeg.get(t.getTaskId()) == 0) {
                    pq.add(t);
                }
            }
        }

        List<ScheduleTask> order = new ArrayList<ScheduleTask>();
        int processed = 0;
        while (!pq.isEmpty()) {
            ScheduleTask cur = pq.poll();
            order.add(cur);
            processed++;
            List<Integer> outs = adj.get(cur.getTaskId());
            if (outs != null) {
                for (Integer v : outs) {
                    indeg.put(v, indeg.get(v) - 1);
                    if (indeg.get(v) == 0) {
                        pq.add(idMap.get(v));
                    }
                }
            }
        }
        if (processed < workCenterTaskIds.size()) {
            throw new IllegalStateException("Cycle detected in work center tasks (partial).");
        }
        return order;
    }
}