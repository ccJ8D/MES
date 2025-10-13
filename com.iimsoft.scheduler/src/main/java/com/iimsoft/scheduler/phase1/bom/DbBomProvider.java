package com.iimsoft.scheduler.phase1.bom;

import com.iimsoft.mes.model.I_mom_bom_component;
import com.iimsoft.mes.model.I_mom_item;
import com.iimsoft.scheduler.common.Component;
import com.iimsoft.util.Services;
import com.iimsofttech.ad.dao.IQueryBL;
import com.iimsofttech.exceptions.IimsofttechException;
import com.iimsofttech.model.InterfaceWrapperHelper;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class DbBomProvider implements BomProvider{
    final IQueryBL queryBL = Services.get(IQueryBL.class);
    @Override
    public List<Component> getComponentsOf(int parentItemId) {
        I_mom_item item = InterfaceWrapperHelper.load(parentItemId, I_mom_item.class);
        if (item == null) {
            throw new IimsofttechException("物料不存在: " + parentItemId);
        }
        List<I_mom_bom_component> components = queryBL.createQueryBuilder(I_mom_bom_component.class)
                .addEqualsFilter(I_mom_bom_component.COLUMNNAME_bom_bo, item.getbom_bo())
                .create().list();
        return components.stream().map(c -> {

            int childId = c.getcomponent_gbo();
            BigDecimal usage = c.getQty() == null ? BigDecimal.ONE : c.getQty();
            return new Component(parentItemId, childId, usage);
        }).collect(Collectors.toList());
    }
}
