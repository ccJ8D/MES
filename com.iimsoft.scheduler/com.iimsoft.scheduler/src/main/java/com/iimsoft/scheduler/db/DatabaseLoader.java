package com.iimsoft.scheduler.db;

// 8. 数据库加载器 - db/DatabaseLoader.java



import com.iimsoft.mes.model.*;
import com.iimsoft.scheduler.model.ProductionShift;
import com.iimsoft.scheduler.model.RouterStep;
import com.iimsoft.scheduler.model.ShopOrder;
import com.iimsoft.util.Services;
import com.iimsofttech.ad.dao.IQueryBL;
import com.iimsofttech.model.InterfaceWrapperHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

public class DatabaseLoader {
    IQueryBL queryBL = Services.get(IQueryBL.class);


    /**
     * 重写后的 loadPendingShopOrders：多级 BOM 展开（递归）
     */
    public List<ShopOrder> loadPendingShopOrders() {
        List<ShopOrder> shopOrders = new ArrayList<>();

        List<I_tt_mar_material_requirement_plan> plans = queryBL.createQueryBuilder(I_tt_mar_material_requirement_plan.class)
                .addEqualsFilter(I_tt_mar_material_requirement_plan.COLUMNNAME_Level, 0)
                .addEqualsFilter(I_tt_mar_material_requirement_plan.COLUMNNAME_qty_type, X_tt_mar_material_requirement_plan.QTY_TYPE_S)
                .create().list();

        Map<Integer, AggregatedEntry> aggregated = new LinkedHashMap<>();


        for (I_tt_mar_material_requirement_plan plan : plans) {
            int partId = plan.getpartid();
            BigDecimal totalQty = getTotalQty(plan);
            if (totalQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            LocalDateTime dueDate = LocalDateTime.of(2025, 11, 1, 0, 0);
            
            I_mom_item item = InterfaceWrapperHelper.load(partId, I_mom_item.class);


            processBomRecursive(item, totalQty, dueDate, dueDate, aggregated);
        }
        // 将 aggregated 转换为 ShopOrder 列表（生成 shopOrder id）
        for (AggregatedEntry e : aggregated.values()) {
            // 计算生产所需小时并转为开始时间（如果未设置 start，则用 due - prodHours）
            BigDecimal prodHours = e.productionHours != null ? e.productionHours : estimateProductionHoursForItem(e.item, e.totalQty);
            LocalDateTime compDate = e.requiredComp != null ? e.requiredComp : LocalDateTime.now();
            LocalDateTime startDate = e.requiredStart != null ? e.requiredStart : compDate.minusHours(prodHours.longValue());

            ShopOrder so = new ShopOrder(
                    e.materialId,
                    e.totalQty,
                    startDate,
                    compDate,
                    e.routerBo,
                    prodHours.intValue(),
                    e.dependentItems
            );
            shopOrders.add(so);
        }
        return shopOrders;
    }

    private void processBomRecursive(I_mom_item parentItem,
                                     BigDecimal parentQty,
                                     LocalDateTime parentStart,
                                     LocalDateTime parentComp,
                                     Map<Integer, AggregatedEntry> aggregated
    ) {
        // 读取父物料的 BOM 行
        I_mom_bom parentBom = InterfaceWrapperHelper.load(parentItem.getbom_bo(), I_mom_bom.class);
        if (parentBom == null) return;

        List<I_mom_bom_component> components = queryBL.createQueryBuilder(I_mom_bom_component.class)
                .addEqualsFilter(I_mom_bom_component.COLUMNNAME_bom_bo, parentBom.getmom_bom_ID())
                .addEqualsFilter(I_mom_bom_component.COLUMNNAME_bom_component_type, X_mom_bom_component.BOM_COMPONENT_TYPE_NormalBOMComponent)
                .create().list();

        if (components == null || components.isEmpty()) return;
        AggregatedEntry parentEntry = aggregated.get(parentItem.getmom_item_ID());

        for (I_mom_bom_component component : components) {
            if ( parentEntry!=null&&!parentEntry.dependentItems.contains(component.getcomponent_gbo())){
                parentEntry.dependentItems.add(component.getcomponent_gbo());
            }

            // 加载子物料项
            I_mom_item childItem = InterfaceWrapperHelper.load(component.getcomponent_gbo(), I_mom_item.class);
            if (childItem == null) continue;

            BigDecimal perQty = component.getQty() != null ? component.getQty() : BigDecimal.ONE;
            BigDecimal requiredQty = parentQty.multiply(perQty);

            // 计算该子件的生产小时（如果可用）
            BigDecimal childProdHours = estimateProductionHoursForItem(childItem, requiredQty);

            // 子件必须在父件开始前完成（即 parentStart）——因此子件的最晚完工时间为 parentStart
            LocalDateTime childStartBy = parentStart.minusHours(childProdHours.longValue());

            // 聚合 key：使用 item 的唯一标识（可改为 material bo）
            int key = childItem.getmom_item_ID();
            AggregatedEntry entry = aggregated.get(key);
            if (entry == null) {
                entry = new AggregatedEntry(childItem.getmom_item_ID(), childItem, requiredQty,
                        childStartBy, parentStart, childProdHours, String.valueOf(childItem.getrouter_bo()));
                aggregated.put(key, entry);
            } else {
                entry.totalQty = entry.totalQty.add(requiredQty);
                if (parentStart.isBefore(entry.requiredComp)) entry.requiredComp = parentStart;
                if (childStartBy.isBefore(entry.requiredStart)) entry.requiredStart = childStartBy;
                entry.productionHours = estimateProductionHoursForItem(childItem, entry.totalQty);
            }
            // 递归处理子件的子件（多级 BOM）
            processBomRecursive(childItem, requiredQty, childStartBy, parentStart, aggregated);
        }
    }


    private BigDecimal estimateProductionHoursForItem(I_mom_item item, BigDecimal qty) {
        if (item == null) return BigDecimal.ZERO;
        List<I_mom_standard_rate> standardRates = queryBL.createQueryBuilder(I_mom_standard_rate.class)
                .addEqualsFilter(I_mom_standard_rate.COLUMNNAME_item_bo, item.getmom_item_ID())
                .addEqualsFilter(I_mom_standard_rate.COLUMNNAME_router_bo, item.getrouter_bo())
                .create().list();
        if (standardRates == null || standardRates.isEmpty()) {
            //默认10小时
            return BigDecimal.TEN;
        }
        I_mom_standard_rate sr = standardRates.get(0);
        BigDecimal time = sr.gettime() != null ? sr.gettime() : BigDecimal.ONE; // 单位：小时（假定）
        BigDecimal quantity = sr.getQuantity() != null ? sr.getQuantity() : BigDecimal.ONE;

        BigDecimal rate;
        try {
            rate = quantity.divide(time, 6, RoundingMode.HALF_UP);
            if (rate.compareTo(BigDecimal.ZERO) == 0) rate = BigDecimal.ONE;
        } catch (ArithmeticException ex) {
            rate = BigDecimal.ONE;
        }
        BigDecimal prodHours = qty.divide(rate, 0, RoundingMode.UP);
        return prodHours;
    }

    private static class AggregatedEntry {
        int materialId;
        I_mom_item item;
        BigDecimal totalQty;
        LocalDateTime requiredStart;
        LocalDateTime requiredComp;
        BigDecimal productionHours;
        String routerBo;
        List<Integer> dependentItems = new ArrayList<>();

        AggregatedEntry(int materialId, I_mom_item item, BigDecimal totalQty,
                        LocalDateTime requiredStart, LocalDateTime requiredComp,
                        BigDecimal productionHours, String routerBo) {
            this.materialId = materialId;
            this.item = item;
            this.totalQty = totalQty;
            this.requiredStart = requiredStart != null ? requiredStart : LocalDateTime.now();
            this.requiredComp = requiredComp != null ? requiredComp : LocalDateTime.now();
            this.productionHours = productionHours != null ? productionHours : BigDecimal.ZERO;
            this.routerBo = routerBo;
        }
    }

    private BigDecimal getTotalQty(I_tt_mar_material_requirement_plan plan) {
        BigDecimal qty1 = plan.getdate_1();
        BigDecimal qty2 = plan.getdate_2();
        BigDecimal qty3 = plan.getdate_3();
        BigDecimal qty4 = plan.getdate_4();
        BigDecimal qty5 = plan.getdate_5();
        BigDecimal qty6 = plan.getdate_6();
        BigDecimal qty7 = plan.getdate_7();
        BigDecimal qty8 = plan.getdate_8();
        BigDecimal qty9 = plan.getdate_9();
        BigDecimal qty10 = plan.getdate_10();
        BigDecimal qty11 = plan.getdate_11();
        BigDecimal qty12 = plan.getdate_12();
        BigDecimal qty13 = plan.getdate_13();
        BigDecimal qty14 = plan.getdate_14();
        BigDecimal qty15 = plan.getdate_15();
        BigDecimal qty16 = plan.getdate_16();
        BigDecimal qty17 = plan.getdate_17();
        BigDecimal qty18 = plan.getdate_18();
        BigDecimal qty19 = plan.getdate_19();
        BigDecimal qty20 = plan.getdate_20();
        BigDecimal qty21 = plan.getdate_21();

        BigDecimal qty22 = plan.getdate_22();
        BigDecimal qty23 = plan.getdate_23();
        BigDecimal qty24 = plan.getdate_24();
        BigDecimal qty25 = plan.getdate_25();
        BigDecimal qty26 = plan.getdate_26();
        BigDecimal qty27 = plan.getdate_27();
        BigDecimal qty28 = plan.getdate_28();
        BigDecimal qty29 = plan.getdate_29();
        BigDecimal qty30 = plan.getdate_30();
        BigDecimal qty31 = plan.getdate_31();

        BigDecimal totalQty = BigDecimal.ZERO;
        if (qty1 != null) totalQty = totalQty.add(qty1);
        if (qty2 != null) totalQty = totalQty.add(qty2);
        if (qty3 != null) totalQty = totalQty.add(qty3);
        if (qty4 != null) totalQty = totalQty.add(qty4);
        if (qty5 != null) totalQty = totalQty.add(qty5);
        if (qty6 != null) totalQty = totalQty.add(qty6);
        if (qty7 != null) totalQty = totalQty.add(qty7);
        if (qty8 != null) totalQty = totalQty.add(qty8);
        if (qty9 != null) totalQty = totalQty.add(qty9);
        if (qty10 != null) totalQty = totalQty.add(qty10);
        if (qty11 != null) totalQty = totalQty.add(qty11);
        if (qty12 != null) totalQty = totalQty.add(qty12);
        if (qty13 != null) totalQty = totalQty.add(qty13);
        if (qty14 != null) totalQty = totalQty.add(qty14);
        if (qty15 != null) totalQty = totalQty.add(qty15);
        if (qty16 != null) totalQty = totalQty.add(qty16);
        if (qty17 != null) totalQty = totalQty.add(qty17);
        if (qty18 != null) totalQty = totalQty.add(qty18);
        if (qty19 != null) totalQty = totalQty.add(qty19);
        if (qty20 != null) totalQty = totalQty.add(qty20);
        if (qty21 != null) totalQty = totalQty.add(qty21);
        if (qty22 != null) totalQty = totalQty.add(qty22);
        if (qty23 != null) totalQty = totalQty.add(qty23);
        if (qty24 != null) totalQty = totalQty.add(qty24);
        if (qty25 != null) totalQty = totalQty.add(qty25);
        if (qty26 != null) totalQty = totalQty.add(qty26);
        if (qty27 != null) totalQty = totalQty.add(qty27);
        if (qty28 != null) totalQty = totalQty.add(qty28);
        if (qty29 != null) totalQty = totalQty.add(qty29);
        if (qty30 != null) totalQty = totalQty.add(qty30);
        if (qty31 != null) totalQty = totalQty.add(qty31);
        return totalQty;
    }
    
    public List<RouterStep> loadRouterSteps(String routerBo) {
        String sql = "SELECT rs.handle, rs.sequence, op.operation, op.resource_bo\n"
        + "FROM mom_router_step rs\n"
        + "JOIN mom_router_operation ro ON rs.handle = ro.router_step_bo\n"
        + "JOIN mom_operation op ON ro.operation_bo = op.handle\n"
        + "WHERE rs.router_bo = ?\n"
        + "ORDER BY rs.sequence";
        return null;
    }
    
    public List<ProductionShift> loadProductionShifts() {
        String sql = "SELECT * FROM mom_production_shift";
        return  null;
    }
}