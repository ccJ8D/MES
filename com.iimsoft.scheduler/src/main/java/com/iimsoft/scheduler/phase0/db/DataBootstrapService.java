package com.iimsoft.scheduler.phase0.db;

import com.iimsoft.mes.model.I_mom_item;
import com.iimsoft.mes.model.I_tt_mar_material_requirement_plan;
import com.iimsoft.mes.model.X_tt_mar_material_requirement_plan;
import com.iimsoft.scheduler.common.DailyDemand;
import com.iimsoft.scheduler.phase0.*;
import com.iimsoft.scheduler.phase1.bom.BomProvider;
import com.iimsoft.scheduler.phase1.bom.CachedBomProvider;
import com.iimsoft.scheduler.phase1.bom.DbBomProvider;
import com.iimsoft.scheduler.phase1.shift.ShiftCalendarService;
import com.iimsoft.util.Services;
import com.iimsofttech.ad.dao.IQueryBL;
import com.iimsofttech.model.InterfaceWrapperHelper;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 预阶段统一入口：
 *  - MEMORY 模式：建议直接使用 MemoryDataBuilder.build() 后无需再调本服务
 *  - DB 模式：这里给出骨架，你可以在 TODO 部分填充批量 SQL + 缓存逻辑
 */
public class DataBootstrapService {
    final IQueryBL queryBL = Services.get(IQueryBL.class);

    public BootstrapContext bootstrapDb() {
        List<DailyDemand>  topDemands =new ArrayList<>();
        List<I_tt_mar_material_requirement_plan> plans = queryBL.createQueryBuilder(I_tt_mar_material_requirement_plan.class)
                .addEqualsFilter(I_tt_mar_material_requirement_plan.COLUMNNAME_Level, 0)
                .addEqualsFilter(I_tt_mar_material_requirement_plan.COLUMNNAME_qty_type, X_tt_mar_material_requirement_plan.QTY_TYPE_P)
                .create().list();

        Set<Integer> allItems = new HashSet<>();
        Set<Integer> allWcs = new HashSet<>();
        WorkCenterResolver wcResolver = new DBWorkCenterResolver();

        // 4. 批量加载班次模板并注册
        ShiftCalendarService calendar = new ShiftCalendarService();


        for (I_tt_mar_material_requirement_plan plan : plans) {
            int partId = plan.getpartid();
            I_mom_item item = InterfaceWrapperHelper.load(partId,I_mom_item.class);
            if (item == null){
                continue;
            }
            allItems.add(partId);
            int workCenterId = wcResolver.getWorkCenterId(partId);
            allWcs.add(workCenterId);
            calendar.registerDailyTemplate(workCenterId);

            BigDecimal totalQty = getTotalQty(plan);
            //每个月的1号
            LocalDate startDate = LocalDate.of(Integer.parseInt(plan.getsupplier_year()),Integer.parseInt( plan.getsupplier_month()),1);
            DailyDemand demand = new DailyDemand(partId, startDate,totalQty);
            topDemands.add(demand);
        }

        BomProvider bomProvider = new CachedBomProvider(new DbBomProvider());
        RateResolver rateResolver = new DBRateResolver();

        return new BootstrapContext(
                topDemands,
                bomProvider,
                wcResolver,
                calendar,
                rateResolver,
                allItems,
                allWcs,
                BootstrapMode.DB
        );
    }

    private BigDecimal getTotalQty(I_tt_mar_material_requirement_plan plan){
        //1 -31
        BigDecimal date1 = plan.getdate_1();
        BigDecimal date2 = plan.getdate_2();
        BigDecimal date3 = plan.getdate_3();
        BigDecimal date4 = plan.getdate_4();
        BigDecimal date5 = plan.getdate_5();
        BigDecimal date6 = plan.getdate_6();
        BigDecimal date7 = plan.getdate_7();
        BigDecimal date8 = plan.getdate_8();
        BigDecimal date9 = plan.getdate_9();
        BigDecimal date10 = plan.getdate_10();
        BigDecimal date11 = plan.getdate_10();
        BigDecimal date12 = plan.getdate_12();
        BigDecimal date13 = plan.getdate_13();
        BigDecimal date14 = plan.getdate_14();
        BigDecimal date15 = plan.getdate_15();
        BigDecimal date16 = plan.getdate_16();
        BigDecimal date17 = plan.getdate_17();
        BigDecimal date18 = plan.getdate_18();
        BigDecimal date19 = plan.getdate_19();
        BigDecimal date20 = plan.getdate_20();
        BigDecimal date21 = plan.getdate_21();
        BigDecimal date22 = plan.getdate_22();
        BigDecimal date23 = plan.getdate_23();
        BigDecimal date24 = plan.getdate_24();
        BigDecimal date25 = plan.getdate_25();
        BigDecimal date26 = plan.getdate_26();
        BigDecimal date27 = plan.getdate_27();
        BigDecimal date28 = plan.getdate_28();
        BigDecimal date29 = plan.getdate_29();
        BigDecimal date30 = plan.getdate_30();
        BigDecimal date31 = plan.getdate_31();
        return (date1 == null ? BigDecimal.ZERO : date1)
                .add(date2 == null ? BigDecimal.ZERO : date2)
                .add(date3 == null ? BigDecimal.ZERO : date3)
                .add(date4 == null ? BigDecimal.ZERO : date4)
                .add(date5 == null ? BigDecimal.ZERO : date5)
                .add(date6 == null ? BigDecimal.ZERO : date6)
                .add(date7 == null ? BigDecimal.ZERO : date7)
                .add(date8 == null ? BigDecimal.ZERO : date8)
                .add(date9 == null ? BigDecimal.ZERO : date9)
                .add(date10 == null ? BigDecimal.ZERO : date10)
                .add(date11 == null ? BigDecimal.ZERO : date11)
                .add(date12 == null ? BigDecimal.ZERO : date12)
                .add(date13 == null ? BigDecimal.ZERO : date13)
                .add(date14 == null ? BigDecimal.ZERO : date14)
                .add(date15 == null ? BigDecimal.ZERO : date15)
                .add(date16 == null ? BigDecimal.ZERO : date16)
                .add(date17 == null ? BigDecimal.ZERO : date17)
                .add(date18 == null ? BigDecimal.ZERO : date18)
                .add(date19 == null ? BigDecimal.ZERO : date19)
                .add(date20 == null ? BigDecimal.ZERO : date20)
                .add(date21 == null ? BigDecimal.ZERO : date21)
                .add(date22 == null ? BigDecimal.ZERO : date22)
                .add(date23 == null ? BigDecimal.ZERO : date23)
                .add(date24 == null ? BigDecimal.ZERO : date24)
                .add(date25 == null ? BigDecimal.ZERO : date25)
                .add(date26 == null ? BigDecimal.ZERO : date26)
                .add(date27 == null ? BigDecimal.ZERO : date27)
                .add(date28 == null ? BigDecimal.ZERO : date28)
                .add(date29 == null ? BigDecimal.ZERO : date29)
                .add(date30 == null ? BigDecimal.ZERO : date30)
                .add(date31 == null ? BigDecimal.ZERO : date31);


    }
}