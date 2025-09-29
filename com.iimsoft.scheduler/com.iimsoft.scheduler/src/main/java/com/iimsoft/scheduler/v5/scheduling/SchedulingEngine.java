package com.iimsoft.scheduler.v5.scheduling;

// 13. 排程引擎 - scheduling/SchedulingEngine.java


import com.iimsoft.scheduler.v5.db.DatabaseLoader;
import com.iimsoft.scheduler.v5.model.*;

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
                    order.getQtyToBuild(), slot[0], slot[1]
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
                    order.getQtyToBuild(), slot[0], slot[1]
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
                order.getQtyToBuild(), slot[0], slot[1]
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
            double learningFactor = 1.0 - (0.05 * Math.log10(order.getQtyToBuild()));
            long totalMinutes = (long) (baseMinutes * learningFactor * order.getQtyToBuild());
            
            durations.put(step.getHandle(), Duration.ofMinutes(totalMinutes));
        }
        
        return durations;
    }
    
    private boolean requiresSafetyStock(RouterStep step) {
        // 实际实现应根据物料类型和工序特性判断
        return step.getSequence() > 1; // 非首道工序可能需要安全库存
    }
}