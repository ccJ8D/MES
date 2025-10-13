package com.iimsoft.scheduler.phase0.memory;

import com.iimsoft.scheduler.common.DailyDemand;
import com.iimsoft.scheduler.common.Component;
import com.iimsoft.scheduler.phase0.*;
import com.iimsoft.scheduler.phase1.bom.BomProvider;
import com.iimsoft.scheduler.phase1.shift.ShiftCalendarService;
import com.iimsoft.scheduler.phase1.shift.ShiftCalendarService.DailyTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;


public class MemoryDataBuilder {

    private final List<DailyDemand> demands = new ArrayList<>();
    private final Map<Integer,List<Component>> bomMap = new LinkedHashMap<>();
    private final Map<Integer,Integer> itemToWc = new LinkedHashMap<>();
    private final Map<Integer,List<DailyTemplate>> wcShifts = new LinkedHashMap<>();
    private final Map<Integer,BigDecimal> itemRates = new LinkedHashMap<>();

    public MemoryDataBuilder addDemand(int itemId, LocalDate day, Number qty) {
        demands.add(new DailyDemand(itemId, day, new BigDecimal(qty.toString())));
        return this;
    }

    public MemoryDataBuilder addBom(int parent, int child, Number usage) {
        bomMap.computeIfAbsent(parent, k -> new ArrayList<>())
              .add(new Component(parent, child, new BigDecimal(usage.toString()),BigDecimal.TEN));
        return this;
    }

    public MemoryDataBuilder mapItemToWC(int itemId, int workCenterId) {
        itemToWc.put(itemId, workCenterId);
        return this;
    }

    public MemoryDataBuilder addShift(int workCenterId, LocalTime start, LocalTime end) {
        wcShifts.computeIfAbsent(workCenterId, k -> new ArrayList<>())
                .add(new DailyTemplate(start, end));
        return this;
    }

    public MemoryDataBuilder itemRate(int itemId, BigDecimal ratePiecesPerHour) {
        itemRates.put(itemId, ratePiecesPerHour);
        return this;
    }

    public BootstrapContext build() {
        // 构造 BomProvider
        BomProvider bomProvider = new BomProvider() {
            @Override
            public List<Component> getComponentsOf(int parentItemId) {
                return bomMap.getOrDefault(parentItemId, Collections.emptyList());
            }
        };
        // 也可：若想复用 InMemoryBomProvider，可反射/扩展；这里直接匿名实现。

        // 构造日历并注册模板
        ShiftCalendarService calendar = new ShiftCalendarService();
        for (Map.Entry<Integer,List<DailyTemplate>> e : wcShifts.entrySet()) {
            // 排序后注册
            List<DailyTemplate> tpls = new ArrayList<>(e.getValue());
            tpls.sort(Comparator.comparing(a -> a.start));
            calendar.registerDailyTemplate(e.getKey(), tpls);
        }

        WorkCenterResolver wcResolver = new InMemoryWorkCenterResolver(itemToWc, 0);
        RateResolver rateResolver = itemRates.isEmpty() ? null : new DefaultRateResolver(itemRates);

        Set<Integer> allItems = new LinkedHashSet<>();
        for (DailyDemand d : demands) allItems.add(d.getItemId());
        // 递归收集子件（只一层，若需要多层可改为 BFS）
        Deque<Integer> q = new ArrayDeque<>(allItems);
        Set<Integer> visited = new HashSet<>(allItems);
        while (!q.isEmpty()) {
            int p = q.poll();
            for (Component c : bomMap.getOrDefault(p, Collections.emptyList())) {
                if (visited.add(c.getChildItemId())) {
                    allItems.add(c.getChildItemId());
                    q.add(c.getChildItemId());
                }
            }
        }

        Set<Integer> allWcs = new LinkedHashSet<>(itemToWc.values());

        return new BootstrapContext(
                Collections.unmodifiableList(demands),
                bomProvider,
                wcResolver,
                calendar,
                rateResolver,
                allItems,
                allWcs,
                BootstrapMode.MEMORY
        );
    }
}