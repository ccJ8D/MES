package com.iimsoft.scheduler.shift;

import java.time.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * 加固版 WorkCalendarService (JDK8):
 *  - 整点粒度
 *  - addWholeHours / subtractWholeHours
 *  - 对齐函数独立: alignForwardToWorkingStart / alignBackwardToWorkingEnd
 *  - 可配置最大前向搜索天数 & 自动模板滚动
 *  - 明确异常信息
 */
public class WorkCalendarService {

    /* ======================= 配置参数 ======================= */
    private final int maxForwardSearchDays;
    private final boolean autoRollTemplate;
    private Consumer<String> diagnosticLogger = null;

    public void setDiagnosticLogger(Consumer<String> logger) {
        this.diagnosticLogger = logger;
    }
    private void log(String msg) {
        if (diagnosticLogger != null) diagnosticLogger.accept("[CAL] " + msg);
    }

    /* ======================= 模板与段结构 ======================= */
    public static final class DailyTemplate {
        public final LocalTime start;
        public final LocalTime end;
        public final boolean crossMidnight;
        public DailyTemplate(LocalTime start, LocalTime end) {
            this.start = Objects.requireNonNull(start);
            this.end = Objects.requireNonNull(end);
            this.crossMidnight = !end.isAfter(start);
        }
        @Override public String toString() { return start + "->" + end + (crossMidnight ? "(+1d)" : ""); }
    }
    private static final class Segment {
        final LocalDateTime start; // 含
        final LocalDateTime end;   // 不含
        Segment(LocalDateTime s, LocalDateTime e) {
            if (!e.isAfter(s)) throw new IllegalArgumentException("Segment end must be after start");
            this.start = s;
            this.end = e;
        }
    }

    /* ======================= 状态存储 ======================= */
    private final Map<Integer, List<DailyTemplate>> wcTemplates = new HashMap<Integer, List<DailyTemplate>>();
    private final Map<String, List<Segment>> dayCache = new HashMap<String, List<Segment>>();

    public WorkCalendarService() {
        this(60, false); // 默认：最多前向搜索60天；不自动滚动
    }
    public WorkCalendarService(int maxForwardSearchDays, boolean autoRollTemplate) {
        this.maxForwardSearchDays = maxForwardSearchDays;
        this.autoRollTemplate = autoRollTemplate;
    }

    public void registerDailyTemplate(int workCenterId, List<DailyTemplate> templates) {
        if (templates == null || templates.isEmpty()) {
            wcTemplates.remove(workCenterId);
        } else {
            templates.sort(Comparator.comparing(t -> t.start));
            wcTemplates.put(workCenterId, templates);
        }
        clearCache(workCenterId);
    }

    public void clearCache(int wc) {
        String prefix = wc + "_";
        dayCache.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public Set<Integer> getRegisteredWorkCenters() {
        return Collections.unmodifiableSet(wcTemplates.keySet());
    }

    /* ======================= 校验覆盖 ======================= */
    public void validateCalendarCoverage(int wc, LocalDate minDay, LocalDate maxDay) {
        if (!wcTemplates.containsKey(wc)) {
            throw new IllegalStateException("Work center " + wc + " has no template registered (needed for "
                    + minDay + " .. " + maxDay + ")");
        }
        // 如果 autoRollTemplate=true，则不再做逐日存在检查
        if (autoRollTemplate) return;
        // 简单抽样检测：首日 / 中点 / 尾日
        LocalDate mid = minDay.plusDays(Math.max(0, (maxDay.toEpochDay() - minDay.toEpochDay()) / 2));
        for (LocalDate d : Arrays.asList(minDay, mid, maxDay)) {
            if (getSegments(wc, d).isEmpty()) {
                throw new IllegalStateException("No segments generated for wc=" + wc + " date=" + d
                        + " (template present=" + wcTemplates.containsKey(wc) + ")");
            }
        }
    }

    /* ======================= 公共主函数 ======================= */

    public LocalDateTime lastShiftEnd(int wc, LocalDate day) {
        List<Segment> segs = getSegments(wc, day);
        if (segs.isEmpty()) {
            throw new IllegalStateException("No segments for wc=" + wc + " on " + day);
        }
        LocalDateTime e = segs.get(segs.size() - 1).end;
        return e.withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * 整点 backward：finish 右开；若为段 start 先回退。
     */
    public LocalDateTime subtractWholeHours(int wc, LocalDateTime finish, int hoursInt) {
        if (hoursInt <= 0) return floorHour(finish);
        LocalDateTime cursor = floorHour(finish);
        cursor = adjustIfAtSegmentStart(wc, cursor);
        int remaining = hoursInt;
        int guard = 0;

        while (remaining > 0) {
            Segment seg = findSegmentContainingExclusiveEnd(wc, cursor);
            if (seg == null) {
                cursor = previousWorkingEnd(wc, cursor);
                cursor = adjustIfAtSegmentStart(wc, cursor);
                continue;
            }
            long minutes = Duration.between(seg.start, cursor).toMinutes();
            int usable = (int) (minutes / 60);
            if (usable <= 0) {
                cursor = previousWorkingEnd(wc, seg.start);
                cursor = adjustIfAtSegmentStart(wc, cursor);
                continue;
            }
            int consume = Math.min(usable, remaining);
            cursor = cursor.minusHours(consume);
            remaining -= consume;

            if (remaining > 0) {
                cursor = adjustIfAtSegmentStart(wc, cursor);
            }
            if (++guard > 20000) {
                throw new IllegalStateException("subtractWholeHours guard overflow wc=" + wc +
                        " finish=" + finish + " remaining=" + remaining);
            }
        }
        return floorHour(cursor);
    }

    /**
     * 整点 forward：start 包含，返回加工完成瞬间（右开）。
     */
    public LocalDateTime addWholeHours(int wc, LocalDateTime start, int hoursInt) {
        LocalDateTime cursor = alignForwardToWorkingStart(wc, start);
        if (hoursInt <= 0) return cursor;
        int remaining = hoursInt;
        int guard = 0;
        while (remaining > 0) {
            Segment seg = findSegmentContainingInclusive(wc, cursor);
            if (seg == null) {
                cursor = nextWorkingStart(wc, cursor);
                continue;
            }
            long minutes = Duration.between(cursor, seg.end).toMinutes();
            int usable = (int)(minutes / 60);
            if (usable <= 0) {
                cursor = nextWorkingStart(wc, seg.end);
                continue;
            }
            int consume = Math.min(usable, remaining);
            cursor = cursor.plusHours(consume);
            remaining -= consume;
            if (remaining > 0 && !cursor.isBefore(seg.end)) {
                cursor = nextWorkingStart(wc, cursor);
            }
            if (++guard > 20000) {
                throw new IllegalStateException("addWholeHours guard overflow wc=" + wc +
                        " start=" + start + " remaining=" + remaining);
            }
        }
        return floorHour(cursor);
    }

    /**
     * 仅对齐，不推进工时。
     */
    public LocalDateTime alignForwardToWorkingStart(int wc, LocalDateTime t) {
        LocalDateTime base = floorHour(t);
        Segment seg = findSegmentContainingInclusive(wc, base);
        if (seg != null) return base;
        // 若 base 在任意段 start 之前则取该 start
        List<Segment> segs = getSegments(wc, base.toLocalDate());
        for (Segment s : segs) {
            if (!s.start.isBefore(base)) {
                return s.start;
            }
        }
        // 否则找下一天
        return nextWorkingStart(wc, base);
    }

    public LocalDateTime alignBackwardToWorkingEnd(int wc, LocalDateTime t) {
        LocalDateTime base = floorHour(t);
        Segment seg = findSegmentContainingExclusiveEnd(wc, base);
        if (seg != null) return base;
        // 若恰是某段 start → 回退上一段 end
        return adjustIfAtSegmentStart(wc, base);
    }

    /* ======================= 内部辅助 ======================= */

    private LocalDateTime adjustIfAtSegmentStart(int wc, LocalDateTime t) {
        List<Segment> segs = getSegments(wc, t.toLocalDate());
        for (Segment s : segs) {
            if (s.start.equals(t)) {
                // 回退上一段
                return previousWorkingEnd(wc, s.start);
            }
        }
        return t;
    }

    private Segment findSegmentContainingExclusiveEnd(int wc, LocalDateTime t) {
        LocalDateTime probe = t.minusSeconds(1);
        List<Segment> segs = getSegments(wc, probe.toLocalDate());
        for (Segment s : segs) {
            if (!probe.isBefore(s.start) && probe.isBefore(s.end)) return s;
        }
        return null;
    }

    private Segment findSegmentContainingInclusive(int wc, LocalDateTime t) {
        List<Segment> segs = getSegments(wc, t.toLocalDate());
        for (Segment s : segs) {
            if (!t.isBefore(s.start) && t.isBefore(s.end)) return s;
        }
        return null;
    }

    private LocalDateTime previousWorkingEnd(int wc, LocalDateTime from) {
        LocalDate day = from.toLocalDate();
        List<Segment> segs = getSegments(wc, day);
        Segment best = null;
        for (Segment s : segs) {
            if (s.end.isBefore(from) || s.end.equals(from)) {
                if (best == null || s.end.isAfter(best.end)) best = s;
            }
        }
        if (best != null) return best.end;
        // 前一天
        LocalDate prev = day.minusDays(1);
        int searched = 0;
        while (searched <= maxForwardSearchDays) {
            List<Segment> prevSegs = getSegments(wc, prev);
            if (!prevSegs.isEmpty()) {
                return prevSegs.get(prevSegs.size() - 1).end;
            }
            prev = prev.minusDays(1);
            searched++;
        }
        throw new IllegalStateException("previousWorkingEnd: cannot find past segment within " +
                maxForwardSearchDays + " days wc=" + wc + " from=" + from);
    }

    private LocalDateTime nextWorkingStart(int wc, LocalDateTime from) {
        LocalDate d = from.toLocalDate();
        // 当前日查找更晚段
        List<Segment> segsToday = getSegments(wc, d);
        for (Segment s : segsToday) {
            if (s.start.isAfter(from) || s.start.equals(from)) return s.start;
        }
        // 向后逐日
        LocalDate cursor = d.plusDays(1);
        int searched = 0;
        while (searched <= maxForwardSearchDays) {
            List<Segment> segs = getSegments(wc, cursor);
            if (!segs.isEmpty()) {
                return segs.get(0).start;
            }
            cursor = cursor.plusDays(1);
            searched++;
        }
        throw new IllegalStateException(
                "nextWorkingStart: exceeded search limit (" + maxForwardSearchDays + " days) from=" + from +
                        " wc=" + wc + ", haveTemplates=" + wcTemplates.containsKey(wc) +
                        ", registeredWCs=" + wcTemplates.keySet() +
                        ". Hint: register templates for horizon or enable autoRollTemplate."
        );
    }

    private List<Segment> getSegments(int wc, LocalDate day) {
        String key = wc + "_" + day;
        List<Segment> cached = dayCache.get(key);
        if (cached != null) return cached;

        List<DailyTemplate> tpls = wcTemplates.getOrDefault(wc, Collections.<DailyTemplate>emptyList());
        List<Segment> segs = new ArrayList<Segment>();
        if (!tpls.isEmpty()) {
            // 如果 autoRollTemplate = true，任何日期都用同一模板映射
            for (DailyTemplate dt : tpls) {
                LocalDateTime s = LocalDateTime.of(day, dt.start);
                LocalDateTime e = LocalDateTime.of(day, dt.end);
                if (dt.crossMidnight) e = e.plusDays(1);
                segs.add(new Segment(s, e));
            }
            segs.sort(Comparator.comparing(a -> a.start));
        }
        dayCache.put(key, segs);
        return segs;
    }

    private static LocalDateTime floorHour(LocalDateTime t) {
        return t.withMinute(0).withSecond(0).withNano(0);
    }
}