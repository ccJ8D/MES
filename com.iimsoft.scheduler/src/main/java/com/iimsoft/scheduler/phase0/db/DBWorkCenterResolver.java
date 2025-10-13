package com.iimsoft.scheduler.phase0.db;

import com.iimsoft.mes.model.I_mom_item;
import com.iimsoft.mes.model.I_mom_router;
import com.iimsoft.mes.model.I_mom_router_step;
import com.iimsoft.scheduler.phase0.WorkCenterResolver;
import com.iimsofttech.model.InterfaceWrapperHelper;

public class DBWorkCenterResolver implements WorkCenterResolver {


    @Override
    public int getWorkCenterId(int itemId) {
        I_mom_item item = InterfaceWrapperHelper.load(itemId, I_mom_item.class);
        I_mom_router router = InterfaceWrapperHelper.load(item.getrouter_bo(), I_mom_router.class);
        if (router == null ||!router.ishas_been_released()){
            return 0;
        }else {
            I_mom_router_step routerStep = InterfaceWrapperHelper.load(router.getentry_router_step_bo(),I_mom_router_step.class);
            return routerStep.getreporting_center_bo();
        }
    }

    @Override
    public boolean hasWorkCenter(int itemId) {
        return WorkCenterResolver.super.hasWorkCenter(itemId);
    }
}
