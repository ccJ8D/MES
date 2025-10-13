package com.iimsoft.scheduler.phase1.bom;

import com.iimsoft.scheduler.common.Component;

import java.util.*;

/**
 * 组合多个 BomProvider（按顺序）：
 *  - 第一个返回非空列表即接受（允许内存覆盖 DB）
 *  - 批量模式：先汇总所有父件，每个父件按顺序从 provider 列表取
 */
public class CompositeBomProvider implements BulkBomProvider {

    private final List<BomProvider> providers;

    public CompositeBomProvider(List<BomProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalArgumentException("providers empty");
        }
        this.providers = new ArrayList<>(providers);
    }

    @Override
    public List<Component> getComponentsOf(int parentItemId) {
        for (BomProvider p : providers) {
            List<Component> list = p.getComponentsOf(parentItemId);
            if (!list.isEmpty()) return list;
        }
        return Collections.emptyList();
    }

    @Override
    public Map<Integer, List<Component>> getComponentsBulk(Collection<Integer> parentItemIds) {
        Map<Integer, List<Component>> result = new LinkedHashMap<>();
        for (Integer pid : parentItemIds) {
            result.put(pid, Collections.emptyList());
        }
        // 如果子 provider 中有 Bulk，可利用，但逻辑简单起见逐父遍历
        for (Integer pid : parentItemIds) {
            for (BomProvider p : providers) {
                List<Component> list = p.getComponentsOf(pid);
                if (!list.isEmpty()) {
                    result.put(pid, list);
                    break;
                }
            }
        }
        return result;
    }
}