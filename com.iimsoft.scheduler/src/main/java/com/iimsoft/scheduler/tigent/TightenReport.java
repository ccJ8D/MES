package com.iimsoft.scheduler.tigent;

import java.math.BigDecimal;
import java.util.*;

public class TightenReport {

    private final int tasksProcessed;
    private final int tasksShifted;
    private final BigDecimal totalSlackBefore;
    private final BigDecimal totalSlackAfter;
    private final Map<Integer, List<String>> messages = new LinkedHashMap<Integer, List<String>>();

    public TightenReport(int tasksProcessed,
                         int tasksShifted,
                         BigDecimal totalSlackBefore,
                         BigDecimal totalSlackAfter,
                         Map<Integer, List<String>> messages) {
        this.tasksProcessed = tasksProcessed;
        this.tasksShifted = tasksShifted;
        this.totalSlackBefore = totalSlackBefore;
        this.totalSlackAfter = totalSlackAfter;
        if (messages != null) this.messages.putAll(messages);
    }

    public int getTasksProcessed() { return tasksProcessed; }
    public int getTasksShifted() { return tasksShifted; }
    public BigDecimal getTotalSlackBefore() { return totalSlackBefore; }
    public BigDecimal getTotalSlackAfter() { return totalSlackAfter; }
    public Map<Integer, List<String>> getMessages() { return messages; }

    public String summary() {
        return "TightenReport{processed=" + tasksProcessed +
                ", shifted=" + tasksShifted +
                ", slackBefore=" + totalSlackBefore +
                ", slackAfter=" + totalSlackAfter + "}";
    }
}