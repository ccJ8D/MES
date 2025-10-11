package com.iimsoft.scheduler.merge;

import com.iimsoft.scheduler.model.ScheduleTask;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 合并条件（Tolerance）：
 *  - same workCenterId
 *  - same itemId
 *  - start/end 在一个“聚类窗口”内（start 最大值 - 最小值 <= toleranceHours 且 end 最大值 - 最小值 <= toleranceHours）
 *  - 最终合并的时间窗口取 min(start) ~ max(end)
 *
 * 注意：
 *  - 合并后需要校验新窗口工时是否足够容纳 ceil(sumQty / rate)
 *  - 这个策略示例只做简单聚类：先按 wc+item 排序，然后滑动窗口聚合。
 */
public class WindowToleranceMergeStrategy implements BatchMergeStrategy {

    private final int toleranceHours;

    public WindowToleranceMergeStrategy(int toleranceHours) {
        this.toleranceHours = toleranceHours <= 0 ? 1 : toleranceHours;
    }

    @Override
    public List<List<ScheduleTask>> findMergeGroups(List<ScheduleTask> tasks) {
        // 先按 wc,item,start 排序
        List<ScheduleTask> sorted = new ArrayList<ScheduleTask>(tasks);
        Collections.sort(sorted, new Comparator<ScheduleTask>() {
            public int compare(ScheduleTask a, ScheduleTask b) {
                int c = Integer.compare(a.getWorkCenterId(), b.getWorkCenterId());
                if (c != 0) return c;
                c = Integer.compare(a.getItemId(), b.getItemId());
                if (c != 0) return c;
                c = a.getStart().compareTo(b.getStart());
                if (c != 0) return c;
                return Integer.compare(a.getTaskId(), b.getTaskId());
            }
        });

        List<List<ScheduleTask>> groups = new ArrayList<List<ScheduleTask>>();
        int n = sorted.size();
        int i = 0;
        while (i < n) {
            // 聚类起点
            ScheduleTask base = sorted.get(i);
            List<ScheduleTask> cluster = new ArrayList<ScheduleTask>();
            cluster.add(base);

            LocalDateTime minStart = base.getStart();
            LocalDateTime maxStart = base.getStart();
            LocalDateTime minEnd = base.getEnd();
            LocalDateTime maxEnd = base.getEnd();

            int j = i + 1;
            while (j < n) {
                ScheduleTask cand = sorted.get(j);
                if (cand.getWorkCenterId() != base.getWorkCenterId()
                        || cand.getItemId() != base.getItemId()) {
                    break;
                }
                LocalDateTime newMinStart = minStart.isBefore(cand.getStart()) ? minStart : cand.getStart();
                LocalDateTime newMaxStart = maxStart.isAfter(cand.getStart()) ? maxStart : cand.getStart();
                LocalDateTime newMinEnd = minEnd.isBefore(cand.getEnd()) ? minEnd : cand.getEnd();
                LocalDateTime newMaxEnd = maxEnd.isAfter(cand.getEnd()) ? maxEnd : cand.getEnd();

                long startDiff = Duration.between(newMinStart, newMaxStart).toHours();
                long endDiff = Duration.between(newMinEnd, newMaxEnd).toHours();

                if (startDiff <= toleranceHours && endDiff <= toleranceHours) {
                    cluster.add(cand);
                    minStart = newMinStart;
                    maxStart = newMaxStart;
                    minEnd = newMinEnd;
                    maxEnd = newMaxEnd;
                    j++;
                } else {
                    break;
                }
            }
            if (cluster.size() >= 2) {
                groups.add(cluster);
            }
            i = (cluster.size() > 1) ? j : (i + 1);
        }

        return groups;
    }
}