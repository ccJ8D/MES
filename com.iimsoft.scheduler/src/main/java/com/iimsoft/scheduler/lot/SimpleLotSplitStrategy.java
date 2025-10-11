package com.iimsoft.scheduler.lot;

import com.iimsoft.scheduler.model.LevelContribution;
import com.iimsoft.scheduler.util.TimeAlignUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 按 requiredBy 精确时间分组；每组超过 maxBatch 切块；保持父任务分配映射。
 */
public class SimpleLotSplitStrategy implements LotSplitStrategy {

    private final BigDecimal maxBatchQty;

    public SimpleLotSplitStrategy(BigDecimal maxBatchQty) {
        this.maxBatchQty = (maxBatchQty == null || maxBatchQty.signum() <= 0)
                ? new BigDecimal("999999999") : maxBatchQty;
    }

    @Override
    public List<LotBatch> split(LotSplitContext ctx) {
        Map<LocalDateTime, List<LevelContribution>> groups = new HashMap<LocalDateTime, List<LevelContribution>>();
        for (LevelContribution c : ctx.getContributions()) {
            LocalDateTime key = TimeAlignUtil.ceilToHour(c.getRequiredBy());
            List<LevelContribution> lst = groups.get(key);
            if (lst == null) {
                lst = new ArrayList<LevelContribution>();
                groups.put(key, lst);
            }
            lst.add(c);
        }

        List<LotBatch> result = new ArrayList<LotBatch>();
        int batchIdx = 0;
        for (Map.Entry<LocalDateTime, List<LevelContribution>> e : groups.entrySet()) {
            LocalDateTime due = e.getKey();
            List<ParentSlice> slices = new ArrayList<ParentSlice>();
            BigDecimal groupTotal = BigDecimal.ZERO;
            for (LevelContribution lc : e.getValue()) {
                slices.add(new ParentSlice(lc.getParentTaskId(), lc.getQuantity()));
                groupTotal = groupTotal.add(lc.getQuantity());
            }
            BigDecimal remaining = groupTotal;
            while (remaining.signum() > 0) {
                BigDecimal lotCap = remaining.min(maxBatchQty);
                BigDecimal toFill = lotCap;
                Map<Integer, BigDecimal> alloc = new LinkedHashMap<Integer, BigDecimal>();
                for (ParentSlice ps : slices) {
                    if (toFill.signum() <= 0) break;
                    if (ps.remaining.signum() <= 0) continue;
                    BigDecimal take = ps.remaining.min(toFill);
                    BigDecimal old = alloc.get(ps.parentTaskId);
                    alloc.put(ps.parentTaskId, old == null ? take : old.add(take));
                    ps.remaining = ps.remaining.subtract(take);
                    toFill = toFill.subtract(take);
                }
                result.add(new LotBatch(batchIdx++, ctx.getItemId(), lotCap, due, alloc));
                remaining = remaining.subtract(lotCap);
            }
        }

        Collections.sort(result, new Comparator<LotBatch>() {
            public int compare(LotBatch o1, LotBatch o2) {
                return o1.getDueDate().compareTo(o2.getDueDate());
            }
        });
        return result;
    }

    private static class ParentSlice {
        final int parentTaskId;
        BigDecimal remaining;
        ParentSlice(int parentTaskId, BigDecimal remaining) {
            this.parentTaskId = parentTaskId;
            this.remaining = remaining;
        }
    }
}