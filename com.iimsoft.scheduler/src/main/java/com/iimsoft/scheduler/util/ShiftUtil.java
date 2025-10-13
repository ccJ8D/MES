package com.iimsoft.scheduler.util;

import com.iimsoft.mes.model.I_mom_item;
import com.iimsoft.mes.model.I_mom_router;
import com.iimsoft.mes.model.I_mom_router_step;
import com.iimsoft.mes.model.I_mom_work_center;
import com.iimsofttech.model.InterfaceWrapperHelper;

public class ShiftUtil {
    static  int workcentId=0;
    public static int getLineByItem(int itemId) {
        try {
            I_mom_item item = InterfaceWrapperHelper.load(itemId,I_mom_item.class);
            if (item == null){
                workcentId++;
                return workcentId;
            }
            I_mom_router router = InterfaceWrapperHelper.load(item.getrouter_bo(), I_mom_router.class);
            if (router == null) {
                return 0;
            }
            if (!router.ishas_been_released()){
                return 0;
            }
            I_mom_router_step step = InterfaceWrapperHelper.load(router.getentry_router_step_bo(), I_mom_router_step.class);
            if (step == null) {
                return 0;
            }
            I_mom_work_center wc = InterfaceWrapperHelper.load(step.getreporting_center_bo(), I_mom_work_center.class);
            return wc.getmom_work_center_parent_id();
        }catch (Exception e){
            return workcentId;
        }finally {
            workcentId++;
        }

    }
}
