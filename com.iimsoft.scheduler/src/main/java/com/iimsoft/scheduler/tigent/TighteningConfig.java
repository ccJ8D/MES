package com.iimsoft.scheduler.tigent;

import java.math.BigDecimal;

/**
 * JIT Tightening 配置
 */
public class TighteningConfig {

    // 是否严格要求 child.end < parent.start (true) 否则 ≤
    private final boolean strictParentBoundary;
    // 当 strictParentBoundary=true 时的默认最小间隔小时 (例如 1 小时)
    private final int strictGapHours;
    // 是否禁止任务被挪到 originalBackwardStart 之前
    private final boolean keepNotEarlierThanOriginalBackward;
    // 最大尝试迭代次数（多资源/回推冲突时限制）
    private final int maxIterations;
    // 是否计算 slack 指标
    private final boolean computeSlack;
    // 如果压缩产生 start < predecessor.end 冲突是否回滚（true=回滚，否则继续前推使其满足）
    private final boolean rollbackOnChainViolation;

    public TighteningConfig(boolean strictParentBoundary,
                            int strictGapHours,
                            boolean keepNotEarlierThanOriginalBackward,
                            int maxIterations,
                            boolean computeSlack,
                            boolean rollbackOnChainViolation) {
        this.strictParentBoundary = strictParentBoundary;
        this.strictGapHours = strictGapHours <= 0 ? 1 : strictGapHours;
        this.keepNotEarlierThanOriginalBackward = keepNotEarlierThanOriginalBackward;
        this.maxIterations = maxIterations <= 0 ? 3 : maxIterations;
        this.computeSlack = computeSlack;
        this.rollbackOnChainViolation = rollbackOnChainViolation;
    }

    public boolean isStrictParentBoundary() { return strictParentBoundary; }
    public int getStrictGapHours() { return strictGapHours; }
    public boolean isKeepNotEarlierThanOriginalBackward() { return keepNotEarlierThanOriginalBackward; }
    public int getMaxIterations() { return maxIterations; }
    public boolean isComputeSlack() { return computeSlack; }
    public boolean isRollbackOnChainViolation() { return rollbackOnChainViolation; }

    public static TighteningConfig defaultConfig() {
        return new TighteningConfig(true, 1, true, 2, true, true);
    }
}