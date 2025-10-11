package com.iimsoft.scheduler.bom;

import com.iimsoft.scheduler.model.Component;

import java.util.List;

/**
 * 提供某父物料的 BOM 组成明细。
 * 后续可换成数据库实现 / 缓存 / 版本有效期控制。
 */
public interface BomProvider {
    /**
     * 返回 parentItemId 的直接子件列表；若无则空列表。
     */
    List<Component> getComponentsOf(int parentItemId);
}