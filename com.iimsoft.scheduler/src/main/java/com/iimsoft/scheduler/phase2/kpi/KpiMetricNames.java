package com.iimsoft.scheduler.phase2.kpi;

/**
 * 统一 KPI 字段名称，避免 magic string。
 * 可在这里集中新增 / 更名。
 */
public final class KpiMetricNames {

    private KpiMetricNames(){}

    // 全局
    public static final String TOTAL_TASKS = "totalTasks";
    public static final String TOTAL_ITEMS = "totalItems";
    public static final String LEVEL_COUNT = "levelCount";  // 若有层级字段
    public static final String TOTAL_QUANTITY = "totalQuantity";

    // 时间/时长
    public static final String MAKESPAN_HOURS = "makespanHours";
    public static final String AVG_TASK_SPAN_HOURS = "avgTaskSpanHours";
    public static final String AVG_PROCESS_HOURS = "avgProcessHours";
    public static final String MEDIAN_SLACK_HOURS = "medianSlackHours";
    public static final String AVG_SLACK_HOURS = "avgSlackHours";
    public static final String SLACK_P95_HOURS = "slackP95Hours";
    public static final String ZERO_SLACK_TASKS = "zeroSlackTasks";

    // 利用率（资源维度会再细化）
    public static final String GLOBAL_UTILIZATION = "globalUtilization";
    public static final String RESOURCE_UTIL_PREFIX = "util.wc.";       // util.wc.<id> = 0.85
    public static final String RESOURCE_LOAD_STDDEV = "resourceLoadStdDev";

    // 批量
    public static final String AVG_BATCH_QTY = "avgBatchQty";
    public static final String AVG_BATCH_HOURS = "avgBatchHours";
    public static final String BATCH_SIZE_P95 = "batchSizeP95";
    public static final String UNIQUE_WINDOWS = "uniqueTimeWindows";

    // 结构
    public static final String MULTI_DAY_TASKS = "multiDayTasks";
    public static final String MAX_CHAIN_DEPTH = "maxChainDepth";

    // 合规
    public static final String DEPENDENCY_VIOLATIONS = "dependencyViolations"; // >0 表示有问题

    // 库存代理（可选：end 到父 start 的平均差 → slack）
    public static final String INVENTORY_TIME_PROXY = "inventoryTimeProxyHoursAvg";
}