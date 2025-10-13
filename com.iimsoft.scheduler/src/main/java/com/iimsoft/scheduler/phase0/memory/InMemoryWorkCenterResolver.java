package com.iimsoft.scheduler.phase0.memory;

import com.iimsoft.scheduler.phase0.WorkCenterResolver;

import java.util.Map;

public class InMemoryWorkCenterResolver implements WorkCenterResolver {

    private final Map<Integer,Integer> itemToWc;
    private final int defaultWc;

    public InMemoryWorkCenterResolver(Map<Integer,Integer> itemToWc, int defaultWc) {
        this.itemToWc = itemToWc;
        this.defaultWc = defaultWc;
    }

    public InMemoryWorkCenterResolver(Map<Integer,Integer> itemToWc) {
        this(itemToWc, 0);
    }

    @Override
    public int getWorkCenterId(int itemId) {
        return itemToWc.getOrDefault(itemId, defaultWc);
    }

    public Map<Integer,Integer> asMap() {
        return itemToWc;
    }
}