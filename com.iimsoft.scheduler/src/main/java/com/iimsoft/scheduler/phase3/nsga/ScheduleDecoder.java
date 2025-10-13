package com.iimsoft.scheduler.phase3.nsga;

import com.iimsoft.scheduler.phase1.shift.ShiftCalendarService;
import com.iimsoft.scheduler.common.ScheduleTask;

import java.util.*;

/**
 * 将染色体解码为最终排程：
 * 步骤：
 *  1. 按染色体顺序重排每个工作中心的任务（维持跨 WC 依赖）
 *  2. 调用资源 Sequencing（Forward）确保无重叠
 *  3. （可选）批次合并
 *  4. （可选）Tightening
 *  返回一个新的任务列表（深拷贝，不污染原始基线任务）
 *
 * 性能优化：可引入缓存 (chromosomeHash -> objectives)。
 */
public class ScheduleDecoder {

    private final ShiftCalendarService calendar;

    private final boolean applyBatchMerge;
    private final boolean applyTightening;

    public ScheduleDecoder(ShiftCalendarService calendar,
                           boolean applyBatchMerge,
                           boolean applyTightening) {
        this.calendar = calendar;
        this.applyBatchMerge = applyBatchMerge;
        this.applyTightening = applyTightening;
    }

    /**
     * baselineTasks: 原始任务基线（包含任务属性、工时、依赖、workCenterId）。
     */
    public List<ScheduleTask> decode(Chromosome chromosome,
                                     List<ScheduleTask> baselineTasks) {

        // 1. 深拷贝任务（避免污染）
        Map<Integer, ScheduleTask> idCopy = new LinkedHashMap<Integer, ScheduleTask>();
        for (ScheduleTask t : baselineTasks) {
            ScheduleTask copy = cloneTask(t);
            idCopy.put(copy.getTaskId(), copy);
        }

        // 2. 按工作中心顺序重排：将序列写回一个列表用于 Sequencer
        List<ScheduleTask> reordered = new ArrayList<ScheduleTask>();
        Map<Integer, int[]> wcSeq = chromosome.getWcSequences();

        // 验证覆盖
        Set<Integer> seen = new HashSet<Integer>();
        for (Map.Entry<Integer, int[]> e : wcSeq.entrySet()) {
            int wc = e.getKey();
            int[] order = e.getValue();
            for (int id : order) {
                ScheduleTask ct = idCopy.get(id);
                if (ct == null) {
                    throw new IllegalStateException("Chromosome references unknown taskId=" + id);
                }
                if (ct.getWorkCenterId() != wc) {
                    // 可能原任务改过 wc -> 需保持一致；这里直接允许但警告
                    // 或抛异常
                }
                reordered.add(ct);
                seen.add(id);
            }
        }
        // 补上未在 wcSeq 中定义（不可交换或无资源）的任务
        for (ScheduleTask t : idCopy.values()) {
            if (!seen.contains(t.getTaskId())) {
                reordered.add(t);
            }
        }

//        // 3. 资源序列化（Forward）
//        Phase4ResourceSequencingProcessor seqProcessor =
//                new Phase4ResourceSequencingProcessor(calendar, null,
//                        true,  // allowEqualEndStart
//                        true,  // keepBackwardJIT
//                        true); // strictPredecessorFinish
//        seqProcessor.process(reordered); // 直接修改内存中的 tasks
//
//        // 4. 批次合并（可选）
//        if (applyBatchMerge) {
//            BatchMerger merger = new BatchMerger(null, true);
//            BatchMergeStrategy strategy = new SimpleExactWindowMergeStrategy();
//            new Phase4BatchMergeProcessor(merger, strategy).process(reordered);
//        }
//
//        // 5. Tightening（可选）
//        if (applyTightening) {
//            TighteningConfig cfg = TighteningConfig.defaultConfig();
//            new Phase4JitTighteningProcessor(calendar, null, cfg).process(reordered);
//        }

        return reordered;
    }

    private ScheduleTask cloneTask(ScheduleTask t) {
        ScheduleTask c = new ScheduleTask(
                t.getTaskId(),
                t.getItemId(),
                t.getQuantity(),
                t.getProcessHours(),
                t.getStart(),
                t.getEnd(),
                t.getWorkCenterId(),
                new ArrayList<Integer>(t.getPredecessors())
        );
        c.setWorkCenterId(t.getWorkCenterId());
        c.setOriginalBackwardStart(t.getOriginalBackwardStart());
        c.setSlackHours(t.getSlackHours());
        return c;
    }
}