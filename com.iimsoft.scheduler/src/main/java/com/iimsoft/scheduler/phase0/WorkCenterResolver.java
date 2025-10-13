package com.iimsoft.scheduler.phase0;

/**
 * 统一解析 itemId -> workCenterId。
 * 说明：
 *  - 当返回 0 或负数表示未配置（上层可决定抛错 / 使用默认 / 跳过）
 *  - 实现可以是内存 Map、数据库批量加载后的缓存、或混合懒加载。
 */
public interface WorkCenterResolver {
    int getWorkCenterId(int itemId);

    /**
     * 是否存在该物料的明确配置（可用于统计缺失项）
     */
    default boolean hasWorkCenter(int itemId) {
        return getWorkCenterId(itemId) > 0;
    }
}