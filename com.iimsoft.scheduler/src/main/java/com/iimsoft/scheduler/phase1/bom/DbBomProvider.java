package com.iimsoft.scheduler.phase1.bom;

import com.iimsoft.mes.model.I_mom_bom_component;
import com.iimsoft.mes.model.I_mom_item;
import com.iimsoft.scheduler.common.Component;
import com.iimsoft.util.Services;
import com.iimsofttech.ad.dao.IQueryBL;
import com.iimsofttech.exceptions.IimsofttechException;
import com.iimsofttech.model.InterfaceWrapperHelper;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据库 BOM 提供者：
 *  - 单父件查询：读取 item -> bom_bo -> 组件
 *  - 批量查询：先批量获取 item -> bom_bo，再一次性查 component
 */
public class DbBomProvider implements BulkBomProvider {

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
            return new Component(parentItemId, childId, usage,BigDecimal.TEN);
        }).collect(Collectors.toList());
    }

    @Override
    public Map<Integer, List<Component>> getComponentsBulk(Collection<Integer> parentItemIds) {
        if (parentItemIds == null || parentItemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 1. 批量加载 items
        Map<Integer, I_mom_item> itemMap = new LinkedHashMap<>();
        for (Integer pid : parentItemIds) {
            I_mom_item it = InterfaceWrapperHelper.load(pid, I_mom_item.class);
            if (it != null) {
                itemMap.put(pid, it);
            }
        }
        if (itemMap.isEmpty()) {
            return Collections.emptyMap();
        }
        // 2. 根据 bom_bo 聚类
        Map<Integer, Integer> bomBoToParent = new HashMap<>();
        for (Map.Entry<Integer, I_mom_item> e : itemMap.entrySet()) {
            bomBoToParent.put(e.getValue().getbom_bo(), e.getKey());
        }
        // 3. 查询所有组件（如果 IQueryBL 支持 IN）
        List<I_mom_bom_component> comps = queryBL.createQueryBuilder(I_mom_bom_component.class)
                .addInArrayFilter(I_mom_bom_component.COLUMNNAME_bom_bo, bomBoToParent.keySet())
                .create().list();

        Map<Integer, List<Component>> result = new LinkedHashMap<>();
        for (I_mom_bom_component c : comps) {
            Integer parentId = bomBoToParent.get(c.getbom_bo());
            if (parentId == null) continue;
            BigDecimal usage = c.getQty() == null ? BigDecimal.ONE : c.getQty();
            Component cmp = new Component(parentId, c.getcomponent_gbo(), usage,BigDecimal.TEN);
            result.computeIfAbsent(parentId, k -> new ArrayList<>()).add(cmp);
        }
        // 确保缺失的父返回空列表
        for (Integer pid : parentItemIds) {
            result.computeIfAbsent(pid, k -> new ArrayList<>());
        }
        return result;
    }


}