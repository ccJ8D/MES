package com.iimsoft.scheduler.model;


import io.swagger.models.auth.In;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
public class ShopOrder {
    private Integer shopOrder;
    private BigDecimal qtyToBuild;
    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedCompDate;
    private String routerBo;
    private boolean pullSystem;

    private int productHours; // 产品工时
    private List<Integer> dependentOrders; // 依赖订单
    
    public ShopOrder(Integer shopOrder, BigDecimal qtyToBuild,
                    LocalDateTime plannedStartDate, LocalDateTime plannedCompDate,
                    String routerBo,int productHours, List<Integer> dependentOrders) {
        this.shopOrder = shopOrder;
        this.qtyToBuild = qtyToBuild;
        this.plannedStartDate = plannedStartDate;
        this.plannedCompDate = plannedCompDate;
        this.routerBo = routerBo;
        this.pullSystem = false; // 默认为推式系统
        this.productHours = productHours;
        this.dependentOrders = dependentOrders;
    }

}