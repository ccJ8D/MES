package com.iimsoft.scheduler.bom;

import com.iimsoft.scheduler.model.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * Phase2 演示用内存 BOM：
 *   10001 (成品) -> 20001 (usage 2), 20002 (usage 1)
 *   20001 -> 30001 (usage 3)
 * 可自行修改。
 */
public class InMemoryBomProvider implements BomProvider {

    private final Map<Integer, List<Component>> index = new HashMap<>();

    public InMemoryBomProvider() {
        // 示例数据
        add(new Component(10001, 20001, new BigDecimal("2")));
        add(new Component(10001, 20002, new BigDecimal("1")));
        add(new Component(20001, 30001, new BigDecimal("3")));
        // 可继续添加
    }

    private void add(Component c) {
        index.computeIfAbsent(c.getParentItemId(), k -> new ArrayList<>()).add(c);
    }

    @Override
    public List<Component> getComponentsOf(int parentItemId) {
        return index.getOrDefault(parentItemId, new ArrayList<>());
    }
}