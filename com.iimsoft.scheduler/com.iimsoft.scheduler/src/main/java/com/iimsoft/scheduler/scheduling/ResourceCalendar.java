package com.iimsoft.scheduler.scheduling;

// 11. 资源日历管理 - scheduling/ResourceCalendar.java

import com.iimsoft.scheduler.model.ProductionShift;
import java.time.*;
import java.util.*;

public class ResourceCalendar {
    private Map<String, List<ProductionShift>> shiftMap = new HashMap<>();
    
    public void initShifts(List<ProductionShift> shifts) {
        // 简化处理：所有资源使用相同班次
        shiftMap.put("DEFAULT", shifts);
    }
    
    public LocalDateTime[] findAvailableSlot(String resourceId, LocalDateTime start, 
                                           Duration duration, boolean forward) {
        // 简化实现：直接返回从start开始的duration时间段
        // 实际实现应考虑班次、休息时间、维护时间等
        LocalDateTime end = start.plus(duration);
        return new LocalDateTime[]{start, end};
    }
    
    public long getAvailableMinutes(String resourceId, LocalDate startDate, LocalDate endDate) {
        // 简化：假设每天8小时工作
        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        return days * 8 * 60; // 分钟
    }
}