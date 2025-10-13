package com.iimsoft.scheduler.phase2.kpi;

import com.iimsoft.scheduler.common.ScheduleTask;
import com.iimsoft.scheduler.util.DependencyIndex;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 核心 KPI 计算：
 *  - 假设任务已经 Sequencing + Merge + Tightening
 *  - 任务 start/end 均在工作时间整点
 */
public class KpiCollector {

    private final List<KpiAugmentor> augmentors;

    public KpiCollector() {
        this.augmentors = new ArrayList<KpiAugmentor>();
    }

    public KpiCollector(List<KpiAugmentor> augmentors) {
        this.augmentors = augmentors == null ? new ArrayList<KpiAugmentor>() : augmentors;
    }

    public KpiReport collect(List<ScheduleTask> tasks) {
        KpiReport report = new KpiReport();
        if (tasks == null || tasks.isEmpty()) {
            KpiSection empty = new KpiSection("global")
                    .add(KpiValue.of("empty", true));
            report.addSection(empty);
            return report;
        }

        DependencyIndex dep = new DependencyIndex(tasks);

        // 1. 全局基础
        report.addSection(buildGlobal(tasks));
        // 2. 资源利用率
        report.addSection(buildResource(tasks));
        // 3. Slack 指标
        report.addSection(buildSlack(tasks, dep));
        // 4. 批量与窗口
        report.addSection(buildBatch(tasks));
        // 5. 依赖与拓扑
        report.addSection(buildDependency(tasks, dep));

        // 6. 自定义扩展
        for (KpiAugmentor aug : augmentors) {
            aug.augment(tasks, report);
        }

        return report;
    }

    private KpiSection buildGlobal(List<ScheduleTask> tasks) {
        KpiSection s = new KpiSection("global");
        int total = tasks.size();
        Set<Integer> items = new HashSet<Integer>();
        BigDecimal totalQty = BigDecimal.ZERO;
        LocalDateTime minStart = null;
        LocalDateTime maxEnd = null;
        long sumSpanHours = 0;
        long multiDay = 0;

        for (ScheduleTask t : tasks) {
            items.add(t.getItemId());
            totalQty = totalQty.add(t.getQuantity());
            if (minStart == null || t.getStart().isBefore(minStart)) minStart = t.getStart();
            if (maxEnd == null || t.getEnd().isAfter(maxEnd)) maxEnd = t.getEnd();
            long spanHours = Duration.between(t.getStart(), t.getEnd()).toHours();
            sumSpanHours += spanHours;
            LocalDate ds = t.getStart().toLocalDate();
            LocalDate de = t.getEnd().toLocalDate();
            if (!ds.equals(de)) multiDay++;
        }
        long makespan = Duration.between(minStart, maxEnd).toHours();
        BigDecimal avgSpan = new BigDecimal(sumSpanHours)
                .divide(new BigDecimal(total), 2, RoundingMode.HALF_UP);

        s.add(KpiValue.of(KpiMetricNames.TOTAL_TASKS, total))
         .add(KpiValue.of(KpiMetricNames.TOTAL_ITEMS, items.size()))
         .add(KpiValue.of(KpiMetricNames.TOTAL_QUANTITY, totalQty))
         .add(KpiValue.of(KpiMetricNames.MAKESPAN_HOURS, makespan, "h", null))
         .add(KpiValue.of(KpiMetricNames.AVG_TASK_SPAN_HOURS, avgSpan, "h", null))
         .add(KpiValue.of(KpiMetricNames.MULTI_DAY_TASKS, multiDay));

        return s;
    }

    private KpiSection buildResource(List<ScheduleTask> tasks) {
        KpiSection s = new KpiSection("resource");
        Map<Integer, List<ScheduleTask>> byWC = new HashMap<Integer, List<ScheduleTask>>();
        for (ScheduleTask t : tasks) {
            byWC.computeIfAbsent(t.getWorkCenterId(), k -> new ArrayList<ScheduleTask>()).add(t);
        }

        // 计算每个资源的利用率 = ΣprocessHours / (spanHours(total earliest start to latest end))
        List<BigDecimal> loads = new ArrayList<BigDecimal>();

        for (Map.Entry<Integer, List<ScheduleTask>> e : byWC.entrySet()) {
            int wc = e.getKey();
            List<ScheduleTask> list = e.getValue();
            BigDecimal procSum = BigDecimal.ZERO;
            LocalDateTime min = null;
            LocalDateTime max = null;
            for (ScheduleTask t : list) {
                procSum = procSum.add(t.getProcessHours());
                if (min == null || t.getStart().isBefore(min)) min = t.getStart();
                if (max == null || t.getEnd().isAfter(max)) max = t.getEnd();
            }
            long span = Duration.between(min, max).toHours();
            BigDecimal util = span <= 0
                    ? BigDecimal.ZERO
                    : procSum.divide(new BigDecimal(span), 4, RoundingMode.HALF_UP);
            loads.add(util);
            s.add(KpiValue.of(KpiMetricNames.RESOURCE_UTIL_PREFIX + wc, util, "ratio", null));
        }

        // 全局利用率：所有任务 process / 全局 makespan * 资源数? 这里采用：Σprocess / Σ各资源跨度
        BigDecimal globalProc = BigDecimal.ZERO;
        BigDecimal denom = BigDecimal.ZERO;
        for (BigDecimal ld : loads) {
            denom = denom.add(BigDecimal.ONE); // 资源数(简化)
        }
        for (ScheduleTask t : tasks) {
            globalProc = globalProc.add(t.getProcessHours());
        }
        BigDecimal globalUtil = denom.signum() == 0 ? BigDecimal.ZERO :
                globalProc.divide(denom, 4, RoundingMode.HALF_UP);

        // 资源负载标准差
        BigDecimal std = stdDev(loads);

        s.add(KpiValue.of(KpiMetricNames.GLOBAL_UTILIZATION, globalUtil, "pseudo", "Σ(hours)/resourceCount"))
         .add(KpiValue.of(KpiMetricNames.RESOURCE_LOAD_STDDEV, std, "ratio", null));

        return s;
    }

    private KpiSection buildSlack(List<ScheduleTask> tasks, DependencyIndex dep) {
        KpiSection s = new KpiSection("slack");
        List<BigDecimal> slacks = new ArrayList<BigDecimal>();
        int zeroSlack = 0;
        for (ScheduleTask t : tasks) {
            // t.slackHours 已在 tightening 里可能设置；若没有则计算
            BigDecimal slack = t.getSlackHours();
            if (slack == null) {
                slack = computeSlackHours(dep, t);
                t.setSlackHours(slack);
            }
            slacks.add(slack);
            if (slack.compareTo(BigDecimal.ZERO) == 0) zeroSlack++;
        }
        Collections.sort(slacks);
        BigDecimal avg = avg(slacks);
        BigDecimal median = percentile(slacks, 50);
        BigDecimal p95 = percentile(slacks, 95);

        s.add(KpiValue.of(KpiMetricNames.AVG_SLACK_HOURS, avg, "h", null))
         .add(KpiValue.of(KpiMetricNames.MEDIAN_SLACK_HOURS, median, "h", null))
         .add(KpiValue.of(KpiMetricNames.SLACK_P95_HOURS, p95, "h", null))
         .add(KpiValue.of(KpiMetricNames.ZERO_SLACK_TASKS, zeroSlack));

        // 库存时间代理（平均 slack）
        s.add(KpiValue.of(KpiMetricNames.INVENTORY_TIME_PROXY, avg, "h", "avg slack as inventory proxy"));
        return s;
    }

    private KpiSection buildBatch(List<ScheduleTask> tasks) {
        KpiSection s = new KpiSection("batch");
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalHours = BigDecimal.ZERO;
        List<BigDecimal> qtyList = new ArrayList<BigDecimal>();
        Set<String> windowSet = new HashSet<String>();

        for (ScheduleTask t : tasks) {
            totalQty = totalQty.add(t.getQuantity());
            totalHours = totalHours.add(t.getProcessHours());
            qtyList.add(t.getQuantity());
            windowSet.add(t.getStart() + "|" + t.getEnd());
        }
        Collections.sort(qtyList, new Comparator<BigDecimal>() {
            public int compare(BigDecimal a, BigDecimal b) {
                return a.compareTo(b);
            }
        });
        BigDecimal avgQty = totalQty.divide(new BigDecimal(tasks.size()), 2, RoundingMode.HALF_UP);
        BigDecimal avgHours = totalHours.divide(new BigDecimal(tasks.size()), 2, RoundingMode.HALF_UP);
        BigDecimal p95Qty = percentile(qtyList, 95);

        s.add(KpiValue.of(KpiMetricNames.AVG_BATCH_QTY, avgQty))
         .add(KpiValue.of(KpiMetricNames.AVG_BATCH_HOURS, avgHours, "h", null))
         .add(KpiValue.of(KpiMetricNames.BATCH_SIZE_P95, p95Qty))
         .add(KpiValue.of(KpiMetricNames.UNIQUE_WINDOWS, windowSet.size()));
        return s;
    }

    private KpiSection buildDependency(List<ScheduleTask> tasks, DependencyIndex dep) {
        KpiSection s = new KpiSection("dependency");
        // 统计前置违例：child.end > parent.start
        int violations = 0;
        for (ScheduleTask child : tasks) {
            Set<ScheduleTask> parents = dep.getParentsOf(child.getTaskId());
            for (ScheduleTask parent : parents) {
                if (child.getEnd().isAfter(parent.getStart())) {
                    violations++;
                }
            }
        }
        // 最大链深度（粗略：DFS）
        int maxDepth = computeMaxDepth(tasks, dep);
        s.add(KpiValue.of(KpiMetricNames.DEPENDENCY_VIOLATIONS, violations))
         .add(KpiValue.of(KpiMetricNames.MAX_CHAIN_DEPTH, maxDepth));
        return s;
    }

    /* ==================== 工具函数 ==================== */
    private BigDecimal computeSlackHours(DependencyIndex dep, ScheduleTask t) {
        Set<ScheduleTask> parents = dep.getParentsOf(t.getTaskId());
        if (parents.isEmpty()) return BigDecimal.ZERO;
        LocalDateTime minParentStart = null;
        for (ScheduleTask p : parents) {
            if (minParentStart == null || p.getStart().isBefore(minParentStart)) {
                minParentStart = p.getStart();
            }
        }
        if (minParentStart == null) return BigDecimal.ZERO;
        long minutes = Duration.between(t.getEnd(), minParentStart).toMinutes();
        if (minutes < 0) minutes = 0;
        return new BigDecimal(minutes).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentile(List<BigDecimal> sorted, int p) {
        if (sorted == null || sorted.isEmpty()) return BigDecimal.ZERO;
        if (p <= 0) return sorted.get(0);
        if (p >= 100) return sorted.get(sorted.size() - 1);
        double rank = (p / 100.0) * (sorted.size() - 1);
        int lo = (int)Math.floor(rank);
        int hi = (int)Math.ceil(rank);
        if (lo == hi) return sorted.get(lo);
        BigDecimal a = sorted.get(lo);
        BigDecimal b = sorted.get(hi);
        double frac = rank - lo;
        return a.add(b.subtract(a).multiply(new BigDecimal(frac))).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal avg(List<BigDecimal> list) {
        if (list.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal b : list) sum = sum.add(b);
        return sum.divide(new BigDecimal(list.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal stdDev(List<BigDecimal> list) {
        if (list.size() <= 1) return BigDecimal.ZERO;
        BigDecimal mean = avg(list);
        BigDecimal varianceSum = BigDecimal.ZERO;
        for (BigDecimal b : list) {
            BigDecimal diff = b.subtract(mean);
            varianceSum = varianceSum.add(diff.multiply(diff));
        }
        BigDecimal v = varianceSum.divide(new BigDecimal(list.size()), 6, RoundingMode.HALF_UP);
        double sqrt = Math.sqrt(v.doubleValue());
        return new BigDecimal(sqrt).setScale(4, RoundingMode.HALF_UP);
    }

    private int computeMaxDepth(List<ScheduleTask> tasks, DependencyIndex dep) {
        Map<Integer, Integer> memo = new HashMap<Integer, Integer>();
        int max = 0;
        for (ScheduleTask t : tasks) {
            int d = depthDfs(t.getTaskId(), dep, memo);
            if (d > max) max = d;
        }
        return max;
    }

    private int depthDfs(int taskId, DependencyIndex dep, Map<Integer, Integer> memo) {
        Integer cached = memo.get(taskId);
        if (cached != null) return cached;
        Set<ScheduleTask> children = dep.getChildrenOf(taskId);
        if (children.isEmpty()) {
            memo.put(taskId, 1);
            return 1;
        }
        int max = 0;
        for (ScheduleTask c : children) {
            int d = depthDfs(c.getTaskId(), dep, memo);
            if (d > max) max = d;
        }
        memo.put(taskId, max + 1);
        return max + 1;
    }
}