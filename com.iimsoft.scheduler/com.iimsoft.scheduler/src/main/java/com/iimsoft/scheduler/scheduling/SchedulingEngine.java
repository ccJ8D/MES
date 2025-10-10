package com.iimsoft.scheduler.scheduling;

// 13. 排程引擎 - scheduling/SchedulingEngine.java


import com.iimsoft.scheduler.db.DatabaseLoader;
import com.iimsoft.scheduler.model.RouterStep;
import com.iimsoft.scheduler.model.ScheduleResult;
import com.iimsoft.scheduler.model.ShopOrder;
import com.iimsoft.scheduler.model.StepSchedule;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class SchedulingEngine {
    private final ResourceCalendar calendar;
    private final DatabaseLoader loader;
    private final Duration SAFETY_STOCK_BUFFER = Duration.ofHours(4);
    private final Duration MIN_INTERVAL = Duration.ofMinutes(30);
    
    public SchedulingEngine(ResourceCalendar calendar, DatabaseLoader loader) {
        this.calendar = calendar;
        this.loader = loader;
    }
    
    public ScheduleResult scheduleOrder(ShopOrder order, String bottleneckResource) {
        if (order.isPullSystem()) {
            return backwardSchedule(order, bottleneckResource);
        } else {
            return forwardSchedule(order, bottleneckResource);
        }
    }
    
    private ScheduleResult forwardSchedule(ShopOrder order, String bottleneckResource) {
        List<RouterStep> steps = loader.loadRouterSteps(order.getRouterBo());
        Map<String, Duration> stepDurations = calculateStepDurations(order, steps);
        LocalDateTime currentTime = order.getPlannedStartDate();
        List<StepSchedule> schedules = new ArrayList<>();
        
        // 瓶颈资源优先排程
        for (RouterStep step : steps) {
            if (step.getResourceBo().equals(bottleneckResource)) {
                Duration duration = stepDurations.get(step.getHandle());
                duration = duration.plus(Duration.ofHours(2)); // 瓶颈保护时间
                
                LocalDateTime[] slot = calendar.findAvailableSlot(
                    step.getResourceBo(), currentTime, duration, true);
                    
                schedules.add(new StepSchedule(
                    step.getSequence(), step.getHandle(), step.getResourceBo(),
                    order.getQtyToBuild().doubleValue(), slot[0], slot[1]
                ));
                currentTime = slot[1].plus(MIN_INTERVAL);
            }
        }
        
        // 非瓶颈资源排程
        for (RouterStep step : steps) {
            if (!step.getResourceBo().equals(bottleneckResource)) {
                Duration duration = stepDurations.get(step.getHandle());
                if (requiresSafetyStock(step)) {
                    duration = duration.plus(SAFETY_STOCK_BUFFER);
                }
                
                LocalDateTime[] slot = calendar.findAvailableSlot(
                    step.getResourceBo(), currentTime, duration, true);
                    
                schedules.add(new StepSchedule(
                    step.getSequence(), step.getHandle(), step.getResourceBo(),
                    order.getQtyToBuild().doubleValue(), slot[0], slot[1]
                ));
                currentTime = slot[1].plus(MIN_INTERVAL);
            }
        }
        
        // 按工序顺序排序
        schedules.sort(Comparator.comparingInt(StepSchedule::getSequence));
        return new ScheduleResult(order.getShopOrder(), schedules);
    }
    
    private ScheduleResult backwardSchedule(ShopOrder order, String bottleneckResource) {
        List<RouterStep> steps = loader.loadRouterSteps(order.getRouterBo());
        Collections.reverse(steps);
        Map<String, Duration> stepDurations = calculateStepDurations(order, steps);
        LocalDateTime currentTime = order.getPlannedCompDate();
        List<StepSchedule> schedules = new ArrayList<>();
        
        for (RouterStep step : steps) {
            Duration duration = stepDurations.get(step.getHandle());
            if (step.getResourceBo().equals(bottleneckResource)) {
                duration = duration.plus(Duration.ofHours(2));
            }
            
            LocalDateTime[] slot = calendar.findAvailableSlot(
                step.getResourceBo(), currentTime, duration, false);
                
            schedules.add(new StepSchedule(
                step.getSequence(), step.getHandle(), step.getResourceBo(),
                order.getQtyToBuild().doubleValue(), slot[0], slot[1]
            ));
            currentTime = slot[0].minus(MIN_INTERVAL);
        }
        
        Collections.reverse(schedules);
        return new ScheduleResult(order.getShopOrder(), schedules);
    }
    
    private Map<String, Duration> calculateStepDurations(ShopOrder order, List<RouterStep> steps) {
        Map<String, Duration> durations = new HashMap<>();
        
        for (RouterStep step : steps) {
            // 简化：基于订单数量和固定效率计算时间
            double baseMinutes = 60.0; // 基础时间（分钟）
            double learningFactor = 1.0 - (0.05 * Math.log10(order.getQtyToBuild().doubleValue()));
            long totalMinutes = (long) (baseMinutes * learningFactor * order.getQtyToBuild().doubleValue());
            
            durations.put(step.getHandle(), Duration.ofMinutes(totalMinutes));
        }
        
        return durations;
    }
    
    private boolean requiresSafetyStock(RouterStep step) {
        // 实际实现应根据物料类型和工序特性判断
        return step.getSequence() > 1; // 非首道工序可能需要安全库存
    }



    // ---------- 新增：依赖传递计算完成时间 ----------
    /**
     * 基于传入工单集合中的依赖关系计算每个工单的完成时间（不按工序仿真）
     * 返回 map: shopOrderId -> finishTime (LocalDateTime)
     */
    public Map<Integer, LocalDateTime> computeCompletionTimesByDependencies(List<ShopOrder> orders) {
        Map<Integer, ShopOrder> orderMap = new HashMap<>();
        for (ShopOrder so : orders) orderMap.put(so.getShopOrder(), so);

        // 构建依赖图（只考虑传入集合内的依赖作为边）
        Map<Integer, Integer> indegree = new HashMap<>();
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (ShopOrder so : orders) {
            Integer id = so.getShopOrder();
            indegree.putIfAbsent(id, 0);
            adj.putIfAbsent(id, new ArrayList<>());
        }
        for (ShopOrder so : orders) {
            Integer id = so.getShopOrder();
            if (so.getDependentOrders() == null) continue;
            for (Integer dep : so.getDependentOrders()) {
                if (orderMap.containsKey(dep)) {
                    // dep -> id
                    adj.computeIfAbsent(dep, k -> new ArrayList<>()).add(id);
                    indegree.put(id, indegree.getOrDefault(id, 0) + 1);
                }
            }
        }

        // Kahn 拓扑排序
        Deque<Integer> queue = new ArrayDeque<>();
        for (Integer id : indegree.keySet()) {
            if (indegree.getOrDefault(id, 0) == 0) queue.add(id);
        }

        Map<Integer, LocalDateTime> finishTimes = new HashMap<>();
        // 初始化 finish times for nodes with no internal deps:
        for (ShopOrder so : orders) {
            if (so.getDependentOrders() == null || so.getDependentOrders().isEmpty()) {
                // start = max(plannedStartDate, now)
                LocalDateTime start = so.getPlannedStartDate() != null ? so.getPlannedStartDate() : LocalDateTime.now();
                Duration dur = durationFromPlannedHours(so.getProductHours());
                LocalDateTime finish = start.plus(dur);
                finishTimes.put(so.getShopOrder(), finish);
                // also write back to ShopOrder
                so.setPlannedStartDate(start);
                so.setPlannedCompDate(finish);
            }
        }

        int processed = 0;
        while (!queue.isEmpty()) {
            Integer id = queue.poll();
            processed++;
            ShopOrder cur = orderMap.get(id);
            // ensure cur has finishTimes entry (for nodes that started with indegree 0 we already set)
            finishTimes.putIfAbsent(id, computeFinishForOrder(cur, finishTimes));
            // propagate
            List<Integer> neighbors = adj.getOrDefault(id, Collections.emptyList());
            for (Integer nb : neighbors) {
                indegree.put(nb, indegree.get(nb) - 1);
                if (indegree.get(nb) == 0) queue.add(nb);
            }
        }

        // If cycle exists (not all nodes processed), handle remaining nodes by computing using available info
        if (processed < orders.size()) {
            // 找出未处理节点（环内节点）
            Set<Integer> remaining = new HashSet<>(orderMap.keySet());
            remaining.removeAll(finishTimes.keySet());
            // 尝试按任意顺序计算（取其上游已知 finishTimes 的最大值作为起点）
            System.err.println("Warning: dependency cycle detected among orders: " + remaining + ". Trying best-effort compute.");
            for (Integer id : remaining) {
                ShopOrder so = orderMap.get(id);
                LocalDateTime finish = computeFinishForOrder(so, finishTimes);
                finishTimes.put(id, finish);
            }
        }

        // 回写到 ShopOrder 对象的 plannedStart/plannedComp（方便后续使用）
        for (ShopOrder so : orders) {
            LocalDateTime finish = finishTimes.get(so.getShopOrder());
            if (finish != null) {
                // compute start = finish - duration
                Duration dur = durationFromPlannedHours(so.getProductHours());
                LocalDateTime start = finish.minus(dur);
                so.setPlannedStartDate(start);
                so.setPlannedCompDate(finish);
            }
        }

        return finishTimes;
    }

    /**
     * 计算单个工单的完成时间：start = max(plannedStartDate (if any), max(deps finish + MIN_INTERVAL))
     * finish = start + duration from plannedProductionHours
     */
    private LocalDateTime computeFinishForOrder(ShopOrder order, Map<Integer, LocalDateTime> knownFinishTimes) {
        // 获取依赖的最大完成时间
        LocalDateTime maxDepFinish = null;
        if (order.getDependentOrders() != null) {
            for (Integer depId : order.getDependentOrders()) {
                LocalDateTime depFinish = knownFinishTimes.get(depId);
                if (depFinish == null) {
                    // 依赖可能是集合外部的工单（外部工单）
                    // 尝试用 orderMap's plannedCompDate? Here we can't access orderMap; caller should have written external plannedCompDate into order instances if available.
                    // Fallback：认为外部依赖已完成（now)
                    depFinish = LocalDateTime.now();
                }
                if (maxDepFinish == null || depFinish.isAfter(maxDepFinish)) maxDepFinish = depFinish;
            }
        }

        LocalDateTime candidate = null;
        if (maxDepFinish != null) candidate = maxDepFinish.plus(MIN_INTERVAL);
        if (order.getPlannedStartDate() != null) {
            if (candidate == null || order.getPlannedStartDate().isAfter(candidate)) candidate = order.getPlannedStartDate();
        }
        if (candidate == null) candidate = LocalDateTime.now();

        Duration dur = durationFromPlannedHours(order.getProductHours());
        return candidate.plus(dur);
    }

    private Duration durationFromPlannedHours(double hours) {
        if (hours <= 0) return Duration.ZERO;
        long minutes = Math.round(hours * 60.0);
        return Duration.ofMinutes(minutes);
    }
}