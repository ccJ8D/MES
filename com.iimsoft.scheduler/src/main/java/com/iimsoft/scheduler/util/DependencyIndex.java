package com.iimsoft.scheduler.util;

import com.iimsoft.scheduler.model.ScheduleTask;

import java.util.*;

/**
 * 构建 parent/child 快速索引:
 *  - predecessors: 对应 "child -> parent"
 *  - childrenIndex:  parent -> children
 *
 * 注意：你的模型中 predecessors 存的是”直接子件任务“还是”父任务“？
 * 在前面设计里：父任务的 predecessors 是子任务（child -> parent 反向表示）。
 * 所以:
 *   predecessors: t.predecessors() => 子任务IDs
 *   childrenIndex: childId -> parentTasks  (我们需要反向)
 *
 * 为了 tightening 计算 parentMinStart，需要 child -> 所有 parent.start。
 */
public class DependencyIndex {

    private final Map<Integer, ScheduleTask> idMap = new HashMap<Integer, ScheduleTask>();
    // childId -> Set of parent tasks
    private final Map<Integer, Set<ScheduleTask>> childToParents = new HashMap<Integer, Set<ScheduleTask>>();
    // parentId -> Set of child tasks
    private final Map<Integer, Set<ScheduleTask>> parentToChildren = new HashMap<Integer, Set<ScheduleTask>>();

    public DependencyIndex(List<ScheduleTask> tasks) {
        for (ScheduleTask t : tasks) {
            idMap.put(t.getTaskId(), t);
        }
        for (ScheduleTask t : tasks) {
            // t.predecessors() 是它的直接子任务
            for (Integer childId : t.getPredecessors()) {
                ScheduleTask child = idMap.get(childId);
                if (child == null) continue;
                // child -> parent
                Set<ScheduleTask> pset = childToParents.get(childId);
                if (pset == null) {
                    pset = new LinkedHashSet<ScheduleTask>();
                    childToParents.put(childId, pset);
                }
                pset.add(t);
                // parent -> child
                Set<ScheduleTask> cset = parentToChildren.get(t.getTaskId());
                if (cset == null) {
                    cset = new LinkedHashSet<ScheduleTask>();
                    parentToChildren.put(t.getTaskId(), cset);
                }
                cset.add(child);
            }
        }
    }

    public Set<ScheduleTask> getParentsOf(int childTaskId) {
        Set<ScheduleTask> s = childToParents.get(childTaskId);
        return s == null ? Collections.<ScheduleTask>emptySet() : s;
    }

    public Set<ScheduleTask> getChildrenOf(int parentTaskId) {
        Set<ScheduleTask> s = parentToChildren.get(parentTaskId);
        return s == null ? Collections.<ScheduleTask>emptySet() : s;
    }

    public Map<Integer, ScheduleTask> getIdMap() { return idMap; }
}