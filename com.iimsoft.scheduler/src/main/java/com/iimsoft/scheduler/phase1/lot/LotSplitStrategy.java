package com.iimsoft.scheduler.phase1.lot;

import java.util.List;

public interface LotSplitStrategy {
    List<LotBatch> split(LotSplitContext ctx);
}