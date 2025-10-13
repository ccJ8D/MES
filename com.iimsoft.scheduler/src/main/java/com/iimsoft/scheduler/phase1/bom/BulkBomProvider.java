package com.iimsoft.scheduler.phase1.bom;

import com.iimsoft.scheduler.common.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 批量 BOM 查询扩展接口。
 * 语义：返回给定父物料集合中每个父的直接子件列表；缺失的父返回空列表。
 */
public interface BulkBomProvider extends BomProvider {

    /**
     * 批量获取多个父件的直接子件。
     * @param parentItemIds 父件集合
     * @return parentId -> 子件列表
     */
    Map<Integer, List<Component>> getComponentsBulk(Collection<Integer> parentItemIds);

    /**
     * 默认降级实现：循环单查（非高效），具体实现可覆盖。
     */
    default Map<Integer, List<Component>> getComponentsBulkFallback(Collection<Integer> parentItemIds) {
        java.util.Map<Integer, List<Component>> map = new java.util.LinkedHashMap<Integer, List<Component>>();
        if (parentItemIds == null) return Collections.emptyMap();
        for (Integer pid : parentItemIds) {
            map.put(pid, getComponentsOf(pid));
        }
        return map;
    }
}