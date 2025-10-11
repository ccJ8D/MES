package com.iimsoft.scheduler.merge;

import com.iimsoft.scheduler.model.ScheduleTask;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 合并条件（Exact）：
 *  - same workCenterId
 *  - same itemId
 *  - same start
 *  - same end
 * 可用于快速减少同窗口多批。
 */
public class SimpleExactWindowMergeStrategy implements BatchMergeStrategy {

    @Override
    public List<List<ScheduleTask>> findMergeGroups(List<ScheduleTask> tasks) {
        Map<Key, List<ScheduleTask>> map = new LinkedHashMap<Key, List<ScheduleTask>>();
        for (ScheduleTask t : tasks) {
            Key k = new Key(t.getWorkCenterId(), t.getItemId(), t.getStart(), t.getEnd());
            List<ScheduleTask> list = map.get(k);
            if (list == null) {
                list = new ArrayList<ScheduleTask>();
                map.put(k, list);
            }
            list.add(t);
        }
        List<List<ScheduleTask>> groups = new ArrayList<List<ScheduleTask>>();
        for (Map.Entry<Key, List<ScheduleTask>> e : map.entrySet()) {
            if (e.getValue().size() >= 2) {
                groups.add(e.getValue());
            }
        }
        return groups;
    }

    private static final class Key {
        final int wc;
        final int item;
        final LocalDateTime start;
        final LocalDateTime end;
        Key(int wc, int item, LocalDateTime start, LocalDateTime end) {
            this.wc = wc;
            this.item = item;
            this.start = start;
            this.end = end;
        }
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key)) return false;
            Key k = (Key) o;
            return wc == k.wc &&
                    item == k.item &&
                    start.equals(k.start) &&
                    end.equals(k.end);
        }
        @Override
        public int hashCode() {
            return Objects.hash(wc, item, start, end);
        }
    }
}