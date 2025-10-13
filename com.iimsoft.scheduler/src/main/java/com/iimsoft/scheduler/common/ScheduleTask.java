package com.iimsoft.scheduler.common;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Phase1 的排程任务：
 *  - 仅顶层总成（后续 Phase2/3 才会加入子件 / predecessors）
 *  - start / end 由 Backward 计算（end = due, start = due - 工作工时(扣除非工作时间)）
 */
@Data
public class ScheduleTask {
    private  int taskId;
    private  int itemId;
    private  BigDecimal quantity;
    private  BigDecimal processHours;
    private  LocalDateTime start;
    private  LocalDateTime end;
    private  List<Integer> predecessors;
    private int workCenterId; // 非构造参数，后续排程时设置

    // Phase4 Tightening 新增：记录原 backward start（可在创建时设置）
    private LocalDateTime originalBackwardStart;
    // 最新一次 tightening 后的 slack（与最近父任务或配置）仅统计用
    private BigDecimal slackHours;

    public ScheduleTask(int taskId,
                        int itemId,
                        BigDecimal quantity,
                        BigDecimal processHours,
                        LocalDateTime start,
                        LocalDateTime end,
                        List<Integer> predecessors) {
        this.taskId = taskId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.processHours = processHours;
        this.start = start;
        this.end = end;
        this.predecessors = predecessors == null ? Collections.emptyList() : predecessors;
    }


    @Override
    public String toString() {
        return "ScheduleTask{" +
                "taskId=" + taskId +
                ", itemId=" + itemId +
                ", qty=" + quantity +
                ", hours=" + processHours +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}