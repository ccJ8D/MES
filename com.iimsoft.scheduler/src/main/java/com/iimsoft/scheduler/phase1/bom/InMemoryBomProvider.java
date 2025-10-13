package com.iimsoft.scheduler.phase1.bom;

import com.iimsoft.scheduler.common.Component;

import java.util.*;

/**
 * 支持：
 *  - 默认示例数据
 *  - 动态 addComponent
 *  - 批量接口（直接组合）
 */
public class InMemoryBomProvider implements BulkBomProvider {

    private final Map<Integer, List<Component>> index = new LinkedHashMap<>();


    public InMemoryBomProvider addComponent(Component c) {
        index.computeIfAbsent(c.getParentItemId(), k -> new ArrayList<>()).add(c);
        return this;
    }

    public InMemoryBomProvider addComponents(Collection<Component> components) {
        if (components != null) {
            for (Component c : components) addComponent(c);
        }
        return this;
    }

    public void clear() {
        index.clear();
    }

    @Override
    public List<Component> getComponentsOf(int parentItemId) {
        return index.getOrDefault(parentItemId, Collections.emptyList());
    }

    @Override
    public Map<Integer, List<Component>> getComponentsBulk(Collection<Integer> parentItemIds) {
        Map<Integer, List<Component>> map = new LinkedHashMap<>();
        if (parentItemIds == null) return map;
        for (Integer pid : parentItemIds) {
            map.put(pid, getComponentsOf(pid));
        }
        return map;
    }
}