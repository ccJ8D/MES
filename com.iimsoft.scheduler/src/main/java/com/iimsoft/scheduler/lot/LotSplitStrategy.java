package com.iimsoft.scheduler.lot;

import java.util.List;

public interface LotSplitStrategy {
    List<LotBatch> split(LotSplitContext ctx);
}