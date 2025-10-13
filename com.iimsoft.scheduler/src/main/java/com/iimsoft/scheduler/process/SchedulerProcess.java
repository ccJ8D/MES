package com.iimsoft.scheduler.process;

import com.iimsoft.process.JavaProcess;
import com.iimsoft.scheduler.common.ScheduleTask;
import com.iimsoft.scheduler.phase0.BootstrapContext;
import com.iimsoft.scheduler.phase0.db.DataBootstrapService;
import com.iimsoft.scheduler.phase1.Phase1Facade;
import com.iimsoft.scheduler.phase2.Phase2Facade;

import java.math.BigDecimal;

public class SchedulerProcess extends JavaProcess {
    @Override
    public String doIt() {
        DataBootstrapService service = new DataBootstrapService();
        BootstrapContext build = service.bootstrapDb();

        Phase1Facade phase1 = new Phase1Facade(build.getCalendar(), build.getRateResolver(),build.getBomProvider(),10, BigDecimal.valueOf(1000),build.getWorkCenterResolver());
        Phase1Facade.Result result1 = phase1.taskBuilding(build.getTopDemands());

        Phase2Facade phase2 = new Phase2Facade(build.getCalendar(), build.getRateResolver(),true,true,true);
        Phase2Facade.Result result2 = phase2.sequence(result1.getAllTasks());
        // === 输出 Phase1 结果（顶层任务、所有子任务、所有任务） ===


        System.out.println("\n==== Phase2 所有任务 ====");
        System.out.printf("%-6s %-6s %-6s %-8s %-8s %-16s %-16s %-10s%n",
                "TID", "Item", "WC", "Qty", "Hours", "Start", "End", "Children");
        for (ScheduleTask t : result2.getAllTasks()) {
            System.out.printf("%-6d %-6d %-6d %-8s %-8s %-16s %-16s %-10s%n",
                    t.getTaskId(), t.getItemId(), t.getWorkCenterId(), t.getQuantity(), t.getProcessHours(),
                    t.getStart(), t.getEnd(), t.getPredecessors());
        }
        return "";
    }
}
