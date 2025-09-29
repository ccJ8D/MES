

-- 销售订单表
CREATE TABLE mom_sales_order (
    sales_order_bo VARCHAR(50) PRIMARY KEY,
    shop_order VARCHAR(36)
);

-- 需求表
CREATE TABLE mom_demand (
    sales_order_bo VARCHAR(50),
    demand_qty NUMERIC(18,3),
    FOREIGN KEY (sales_order_bo) REFERENCES mom_sales_order(sales_order_bo)
);

-- 库存曲线表
CREATE TABLE mom_schedule_inventory_curve (
    mom_schedule_inventory_curve_id NUMERIC(10,0) PRIMARY KEY,
    solution_id NUMERIC(10,0),
    timestamp TIMESTAMP NOT NULL,
    inventory_level NUMERIC(18,3),
    note TEXT
);

-- 资源利用率历史表
CREATE TABLE resource_utilization_history (
    resource_utilization_history_id NUMERIC(10,0) PRIMARY KEY,
    resource_id VARCHAR(50),
    date DATE,
    utilization NUMERIC(5,2)
);

-- 资源换线时间表
CREATE TABLE resource_changeover (
    resource_id VARCHAR(50),
    changeover_date TIMESTAMP,
    changeover_time NUMERIC(10,2)
);