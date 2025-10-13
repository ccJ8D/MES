package com.iimsoft.scheduler.phase1;

import com.iimsoft.scheduler.common.Component;
import com.iimsoft.scheduler.common.DailyDemand;
import com.iimsoft.scheduler.common.ScheduleTask;
import com.iimsoft.scheduler.phase0.RateResolver;
import com.iimsoft.scheduler.phase0.WorkCenterResolver;
import com.iimsoft.scheduler.phase1.bom.BomDailyExpander;
import com.iimsoft.scheduler.phase1.bom.BomProvider;
import com.iimsoft.scheduler.phase1.model.ChildDailyDemandTable;
import com.iimsoft.scheduler.phase1.shift.ShiftCalendarService;
import com.iimsoft.scheduler.phase1.model.TopLevelTaskBuilder;
import com.iimsoft.scheduler.phase1.model.ChildLevelScheduler;
import com.iimsoft.scheduler.phase1.model.LevelContribution;
import com.iimsoft.scheduler.phase1.lot.LotSplitStrategy;
import com.iimsoft.scheduler.phase1.lot.SimpleLotSplitStrategy;
import com.iimsoft.scheduler.util.TimeAlignUtil;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;



public class Phase1Facade {

    @Getter
    public static class Result {
        private final List<ScheduleTask> allTasks;//所有的任务
        private final List<ScheduleTask> topLevelTasks; //所有顶级任务
        private final List<ScheduleTask> childTasks; //所有子任务
        private final ChildDailyDemandTable childDailyDemandTable; //所有子件的日需求表

        public Result(List<ScheduleTask> allTasks,
                      List<ScheduleTask> topLevelTasks,
                      List<ScheduleTask> childTasks,
                      ChildDailyDemandTable childDailyDemandTable) {
            this.allTasks = allTasks;
            this.topLevelTasks = topLevelTasks;
            this.childTasks = childTasks;
            this.childDailyDemandTable = childDailyDemandTable;
        }

    }


    private final TopLevelTaskBuilder topLevelTaskBuilder;
    private final BomDailyExpander bomDailyExpander;
    private final BomProvider bomProvider;

    private final ShiftCalendarService calendar;
    private final RateResolver rateResolver;
    private final LotSplitStrategy lotSplitStrategy;

    private final int maxDepth;
    private final BigDecimal maxBatchQty;

    private final WorkCenterResolver workCenterResolver;

    public Phase1Facade(ShiftCalendarService calendar,
                        RateResolver rateResolver,
                        BomProvider bomProvider,
                        int maxDepth,
                        BigDecimal maxBatchQty, WorkCenterResolver workCenterResolver
    ) {
        this.calendar = calendar;
        this.rateResolver = rateResolver;


        this.bomProvider = bomProvider;
        this.maxDepth = maxDepth <= 0 ? 10 : maxDepth;
        this.maxBatchQty = (maxBatchQty == null || maxBatchQty.signum() <= 0)
                ? new BigDecimal("999999") : maxBatchQty;
        this.workCenterResolver = workCenterResolver;


        this.topLevelTaskBuilder = new TopLevelTaskBuilder(calendar, rateResolver,workCenterResolver);

        this.bomDailyExpander = new BomDailyExpander(bomProvider, this.maxDepth);

        this.lotSplitStrategy = new SimpleLotSplitStrategy(this.maxBatchQty);
    }


    public Result taskBuilding(List<DailyDemand> topLevelDailyDemands){

        // 1. 构造顶层任务
        List<ScheduleTask> topTasks = topLevelTaskBuilder.build(topLevelDailyDemands);
        // 2. 计算所有子件的日需求表
        ChildDailyDemandTable childQtyTable = bomDailyExpander.expand(topLevelDailyDemands);


        List<ScheduledNode> currentLevel = new ArrayList<>();
        for (ScheduleTask t : topTasks) {
            currentLevel.add(new ScheduledNode(t, 0));
        }

        List<ScheduleTask> allChildTasks = new ArrayList<>();
        Map<Integer, Set<Integer>> parentToChildrenGlobal = new HashMap<>();

        int nextTaskId = findMaxTaskId(topTasks) + 1;
        int depth = 0;

        ChildLevelScheduler levelScheduler = new ChildLevelScheduler(calendar, rateResolver, lotSplitStrategy,workCenterResolver);

        while (!currentLevel.isEmpty() && depth < maxDepth) {
            List<LevelContribution> contributions = buildDirectChildContributions(currentLevel);

            if (contributions.isEmpty()) {
                break;
            }

            // 3.2 调度该层子件（拆批 + backward）
            ChildLevelScheduler.LayerResult layerResult =
                    levelScheduler.scheduleLevel(contributions, nextTaskId);

            List<ScheduleTask> newChildren = layerResult.newChildTasks;
            nextTaskId = nextTaskId + newChildren.size();

            // 3.3 记录依赖：父任务 predecessors += 子任务
            mergeParentChildLinks(parentToChildrenGlobal, layerResult.parentToChildren);

            // 3.4 新层作为下一轮父层
            List<ScheduledNode> nextLevel = new ArrayList<>();
            for (ScheduleTask child : newChildren) {
                nextLevel.add(new ScheduledNode(child, depth + 1));
            }

            allChildTasks.addAll(newChildren);
            currentLevel = nextLevel;
            depth++;
        }

        // 4 回写父任务 predecessors（只写顶层和中间层：即所有 tasks）
        Map<Integer, ScheduleTask> idMap = new HashMap<Integer, ScheduleTask>();
        for (ScheduleTask t : topTasks) idMap.put(t.getTaskId(), t);
        for (ScheduleTask t : allChildTasks) idMap.put(t.getTaskId(), t);


        List<ScheduleTask> rebuiltTop = new ArrayList<ScheduleTask>();
        for (ScheduleTask t : topTasks) {
            Set<Integer> deps = parentToChildrenGlobal.get(t.getTaskId());
            if (deps == null || deps.isEmpty()) {
                rebuiltTop.add(t);
            } else {
                rebuiltTop.add(new ScheduleTask(
                        t.getTaskId(),
                        t.getItemId(),
                        t.getQuantity(),
                        t.getProcessHours(),
                        t.getStart(),
                        t.getEnd(),
                        t.getWorkCenterId(),
                        new ArrayList<Integer>(deps)
                ));
            }
        }

        // 中间层：那些作为“父”又不是顶层的子件任务也需要 predecessors
        List<ScheduleTask> rebuiltChildren = new ArrayList<ScheduleTask>();
        for (ScheduleTask c : allChildTasks) {
            Set<Integer> deps = parentToChildrenGlobal.get(c.getTaskId());
            if (deps == null || deps.isEmpty()) {
                rebuiltChildren.add(c);
            } else {
                rebuiltChildren.add(new ScheduleTask(
                        c.getTaskId(),
                        c.getItemId(),
                        c.getQuantity(),
                        c.getProcessHours(),
                        c.getStart(),
                        c.getEnd(),
                        c.getWorkCenterId(),
                        new ArrayList<Integer>(deps)
                ));
            }
        }

        List<ScheduleTask> all = new ArrayList<ScheduleTask>();
        all.addAll(rebuiltTop);
        all.addAll(rebuiltChildren);

        // 5 可选：一致性校验
        // assertPredecessorOrder(all); // 可启用

        return new Result(all, rebuiltTop, rebuiltChildren, childQtyTable);
    }

    private int findMaxTaskId(List<ScheduleTask> tasks) {
        int m = 0;
        for (ScheduleTask t : tasks) {
            if (t.getTaskId() > m) m = t.getTaskId();
        }
        return m;
    }

    /**
     * 针对当前父层任务构造“直接子件”贡献列表。
     * 对同一子件 &（不同父）后面拆批再聚合，这里逐个父添加即可。
     */
    private List<LevelContribution> buildDirectChildContributions(List<ScheduledNode> parentNodes) {
        List<LevelContribution> list = new ArrayList<LevelContribution>();
        // 为检测环：父 itemId -> path set（简单防御：若出现 child == 祖先 itemId 抛异常）
        // 这里假设 BOM Provider 自己避免环；如需更严格可扩展 visited map。
        for (ScheduledNode node : parentNodes) {
            ScheduleTask parent = node.task;
            int parentLevel = node.level;
            List<Component> comps = bomProvider.getComponentsOf(parent.getItemId());
            if (comps.isEmpty()) continue;

            for (Component c : comps) {
                // 子件工单数量：父 qty * usage
                BigDecimal childQty = parent.getQuantity().multiply(c.getUsage());

                // 子件 requiredBy = 该父 start - buffer
                LocalDateTime requiredBy = parent.getStart();
                if (c.getBufferHours().signum() > 0) {
                    int buf = c.getBufferHours().setScale(0, RoundingMode.UP).intValue();
                    requiredBy = requiredBy.minusHours(buf);
                }
                requiredBy = TimeAlignUtil.ceilToHour(requiredBy);

                list.add(new LevelContribution(
                        c.getChildItemId(),
                        childQty,
                        requiredBy,
                        parent.getTaskId(),
                        parentLevel + 1
                ));
            }
        }

        // 关键：同一子件多个父任务可能有不同 requiredBy（取最早确保齐套）
        // 这里不在此阶段合并，而是靠 lotSplitGrouping 时按 requiredBy 精确分组处理。
        // 如果你希望“同一子件多个 requiredBy 只保留最早”，可在此预处理：
        // list = compressToEarliest(list);

        return list;
    }

    private void mergeParentChildLinks(Map<Integer, Set<Integer>> global,
                                       Map<Integer, Set<Integer>> layerMap) {
        for (Map.Entry<Integer, Set<Integer>> e : layerMap.entrySet()) {
            Set<Integer> set = global.get(e.getKey());
            if (set == null) {
                set = new LinkedHashSet<Integer>();
                global.put(e.getKey(), set);
            }
            set.addAll(e.getValue());
        }
    }

    // 可选：校验所有前置结束 < 当前开始
    @SuppressWarnings("unused")
    private void assertPredecessorOrder(List<ScheduleTask> tasks) {
        Map<Integer, ScheduleTask> idMap = new HashMap<Integer, ScheduleTask>();
        for (ScheduleTask t : tasks) idMap.put(t.getTaskId(), t);
        List<String> violations = new ArrayList<String>();
        for (ScheduleTask t : tasks) {
            for (Integer preId : t.getPredecessors()) {
                ScheduleTask pre = idMap.get(preId);
                if (pre == null) continue;
                if (!pre.getEnd().isBefore(t.getStart())) {
                    violations.add(preId + "->" + t.getTaskId() + " preEnd=" + pre.getEnd()
                            + " start=" + t.getStart());
                }
            }
        }
        if (!violations.isEmpty()) {
            System.out.println("[WARN] Predecessor violations: " + violations.size());
            for (String v : violations) System.out.println("  " + v);
        }
    }


    //任务层级节点
    private static class ScheduledNode {
        final ScheduleTask task;
        final int level;
        ScheduledNode(ScheduleTask task, int level) {
            this.task = task;
            this.level = level;
        }
    }
}
