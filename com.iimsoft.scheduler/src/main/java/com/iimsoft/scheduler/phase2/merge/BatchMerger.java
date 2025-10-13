package com.iimsoft.scheduler.phase2.merge;

import com.iimsoft.scheduler.common.ScheduleTask;
import com.iimsoft.scheduler.phase1.service.RateService;

import java.math.BigDecimal;
import java.util.*;

/**
 * 根据策略找到可合并组并执行合并：
 *   - 合并后：保留组内最小 taskId 的任务作为“主任务”
 *   - 主任务数量 = Σ数量；工时重新用 rateService.computeProcessHoursCeil
 *   - start = 组内最小 start；end = 组内最大 end（ExactWindow 策略中二者相同）
 *   - predecessors 合并：取并集（去重）
 *   - 被合并的其它任务从最终结果中剔除
 *   - 生成 MergeReport
 *
 * 可扩展：
 *   - 校验窗口容量：若 (end-start) 的可用小时 < 新工时，则拒绝合并 / 或拆分
 *   - 保留 original parentQtyMap：需要 ScheduleTask 暴露 map
 */
public class BatchMerger {

    private final RateService rateService;
    private final boolean verifyWindowCapacity; // 如果要严格校验窗口是否足够装下新工时

    public BatchMerger(RateService rateService,
                       boolean verifyWindowCapacity) {
        this.rateService = rateService;
        this.verifyWindowCapacity = verifyWindowCapacity;
    }

    public MergeReport merge(List<ScheduleTask> tasks,
                             BatchMergeStrategy strategy) {

        List<List<ScheduleTask>> groups = strategy.findMergeGroups(tasks);
        if (groups.isEmpty()) {
            return new MergeReport(tasks.size(), tasks.size(), 0,
                    Collections.<Integer, List<Integer>>emptyMap(),
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // 使用 LinkedHashMap 保持原顺序
        Map<Integer, ScheduleTask> idIndex = new LinkedHashMap<Integer, ScheduleTask>();
        for (ScheduleTask t : tasks) {
            idIndex.put(t.getTaskId(), t);
        }

        Map<Integer, List<Integer>> mergedMap = new LinkedHashMap<Integer, List<Integer>>();
        BigDecimal totalMergedQty = BigDecimal.ZERO;
        BigDecimal totalResultQty = BigDecimal.ZERO;

        // 标记被删除的 ID
        Set<Integer> removed = new HashSet<Integer>();

        for (List<ScheduleTask> group : groups) {
            // 按 taskId 排序，保留最小 id 作为主
            Collections.sort(group, new Comparator<ScheduleTask>() {
                public int compare(ScheduleTask a, ScheduleTask b) {
                    return Integer.compare(a.getTaskId(), b.getTaskId());
                }
            });
            ScheduleTask master = group.get(0);
            if (removed.contains(master.getTaskId())) {
                // 该任务已在其他组合并（理论不应发生，策略应不交叉）
                continue;
            }

            BigDecimal sumQty = BigDecimal.ZERO;
            java.time.LocalDateTime minStart = master.getStart();
            java.time.LocalDateTime maxEnd = master.getEnd();
            Set<Integer> mergedPreds = new LinkedHashSet<Integer>();
            mergedPreds.addAll(master.getPredecessors());
            List<Integer> originalIds = new ArrayList<Integer>();
            originalIds.add(master.getTaskId());

            for (int i = 0; i < group.size(); i++) {
                ScheduleTask t = group.get(i);
                sumQty = sumQty.add(t.getQuantity());
                if (t.getStart().isBefore(minStart)) minStart = t.getStart();
                if (t.getEnd().isAfter(maxEnd)) maxEnd = t.getEnd();
                mergedPreds.addAll(t.getPredecessors());
                if (i > 0) {
                    originalIds.add(t.getTaskId());
                    removed.add(t.getTaskId());
                }
            }

            // 校验窗口容量（可选）
            BigDecimal newHours = rateService.computeProcessHoursCeil(sumQty);
            int hoursInt = newHours.intValue();
            long spanHours = java.time.Duration.between(minStart, maxEnd).toHours();

            if (verifyWindowCapacity && spanHours < hoursInt) {
                // 容量不足，跳过合并（也可“部分合并”）
                continue;
            }

            // 更新 master
            master.setQuantity(sumQty);
            master.setProcessHours(newHours);
            master.setStart(minStart);
            master.setEnd(maxEnd);
            master.setPredecessors(new ArrayList<Integer>(mergedPreds));

            mergedMap.put(master.getTaskId(), originalIds);
            totalMergedQty = totalMergedQty.add(sumQty);
            totalResultQty = totalResultQty.add(sumQty);
        }

        // 构建结果任务列表
        List<ScheduleTask> result = new ArrayList<ScheduleTask>();
        for (ScheduleTask t : tasks) {
            if (!removed.contains(t.getTaskId())) {
                result.add(t);
            }
        }

        return new MergeReport(
                tasks.size(),
                result.size(),
                mergedMap.size(),
                mergedMap,
                totalMergedQty,
                totalResultQty
        );
    }
}