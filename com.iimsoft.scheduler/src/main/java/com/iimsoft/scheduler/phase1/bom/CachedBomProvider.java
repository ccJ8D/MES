package com.iimsoft.scheduler.phase1.bom;

import com.iimsoft.scheduler.common.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class CachedBomProvider implements BulkBomProvider {

    private final BomProvider delegate;
    private final Map<Integer, List<Component>> cache = new ConcurrentHashMap<>();

    public CachedBomProvider(BomProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<Component> getComponentsOf(int parentItemId) {
        return cache.computeIfAbsent(parentItemId, delegate::getComponentsOf);
    }

    @Override
    public Map<Integer, List<Component>> getComponentsBulk(Collection<Integer> parentItemIds) {
        List<Integer> toLoad = new ArrayList<>();
        for (Integer pid : parentItemIds) {
            if (!cache.containsKey(pid)) {
                toLoad.add(pid);
            }
        }
        if (!toLoad.isEmpty() && delegate instanceof BulkBomProvider) {
            Map<Integer, List<Component>> bulk = ((BulkBomProvider) delegate).getComponentsBulk(toLoad);
            for (Map.Entry<Integer, List<Component>> e : bulk.entrySet()) {
                cache.putIfAbsent(e.getKey(), e.getValue());
            }
        } else {
            for (Integer pid : toLoad) {
                cache.putIfAbsent(pid, delegate.getComponentsOf(pid));
            }
        }
        Map<Integer, List<Component>> result = new LinkedHashMap<>();
        for (Integer pid : parentItemIds) {
            result.put(pid, cache.getOrDefault(pid, Collections.emptyList()));
        }
        return result;
    }

    public void preloadParents(Collection<Integer> parentItemIds) {
        getComponentsBulk(parentItemIds);
    }

    public void clear() {
        cache.clear();
    }
}