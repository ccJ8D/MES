package com.iimsoft.scheduler.merge;

import java.math.BigDecimal;
import java.util.*;

public class MergeReport {

    private final int originalTaskCount;
    private final int mergedTaskCount;
    private final int mergeGroupCount;
    private final Map<Integer, List<Integer>> mergedTaskIdToOriginals = new LinkedHashMap<Integer, List<Integer>>();
    private final BigDecimal totalMergedQuantity; // 合并涉及的被合并任务总产量
    private final BigDecimal totalRemainingQuantity; // 合并后新任务的总产量（应等于上者）

    public MergeReport(int originalTaskCount,
                       int mergedTaskCount,
                       int mergeGroupCount,
                       Map<Integer, List<Integer>> mergedMap,
                       BigDecimal totalMergedQuantity,
                       BigDecimal totalRemainingQuantity) {
        this.originalTaskCount = originalTaskCount;
        this.mergedTaskCount = mergedTaskCount;
        this.mergeGroupCount = mergeGroupCount;
        if (mergedMap != null) {
            this.mergedTaskIdToOriginals.putAll(mergedMap);
        }
        this.totalMergedQuantity = totalMergedQuantity;
        this.totalRemainingQuantity = totalRemainingQuantity;
    }

    public int getOriginalTaskCount() { return originalTaskCount; }
    public int getMergedTaskCount() { return mergedTaskCount; }
    public int getMergeGroupCount() { return mergeGroupCount; }
    public Map<Integer, List<Integer>> getMergedTaskIdToOriginals() { return mergedTaskIdToOriginals; }
    public BigDecimal getTotalMergedQuantity() { return totalMergedQuantity; }
    public BigDecimal getTotalRemainingQuantity() { return totalRemainingQuantity; }

    public String summary() {
        double reduction = originalTaskCount == 0 ? 0 :
                (originalTaskCount - mergedTaskCount) * 100.0 / originalTaskCount;
        return "MergeReport{orig=" + originalTaskCount +
                ", after=" + mergedTaskCount +
                ", groups=" + mergeGroupCount +
                ", reduction=" + String.format("%.2f", reduction) + "%}";
    }
}