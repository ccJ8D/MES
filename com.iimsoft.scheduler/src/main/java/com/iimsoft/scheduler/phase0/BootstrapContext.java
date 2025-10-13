package com.iimsoft.scheduler.phase0;

import com.iimsoft.scheduler.common.DailyDemand;
import com.iimsoft.scheduler.phase1.bom.BomProvider;
import com.iimsoft.scheduler.phase1.shift.ShiftCalendarService;


import java.util.List;
import java.util.Set;

public class BootstrapContext {

    private final List<DailyDemand> topDemands;
    private final BomProvider bomProvider;
    private final WorkCenterResolver workCenterResolver;
    private final ShiftCalendarService calendar;
    private final RateResolver rateResolver; // 可为空
    private final Set<Integer> allItemIds;
    private final Set<Integer> allWorkCenterIds;
    private final BootstrapMode mode;

    public BootstrapContext(List<DailyDemand> topDemands,
                            BomProvider bomProvider,
                            WorkCenterResolver workCenterResolver,
                            ShiftCalendarService calendar,
                            RateResolver rateResolver,
                            Set<Integer> allItemIds,
                            Set<Integer> allWorkCenterIds,
                            BootstrapMode mode) {
        this.topDemands = topDemands;
        this.bomProvider = bomProvider;
        this.workCenterResolver = workCenterResolver;
        this.calendar = calendar;
        this.rateResolver = rateResolver;
        this.allItemIds = allItemIds;
        this.allWorkCenterIds = allWorkCenterIds;
        this.mode = mode;
    }

    public List<DailyDemand> getTopDemands() { return topDemands; }
    public BomProvider getBomProvider() { return bomProvider; }
    public WorkCenterResolver getWorkCenterResolver() { return workCenterResolver; }
    public ShiftCalendarService getCalendar() { return calendar; }
    public RateResolver getRateResolver() { return rateResolver; }
    public Set<Integer> getAllItemIds() { return allItemIds; }
    public Set<Integer> getAllWorkCenterIds() { return allWorkCenterIds; }
    public BootstrapMode getMode() { return mode; }
}