CREATE TABLE `tbl_quote_sheet` (
    `id` varchar(32) NOT NULL COMMENT 'ID',
    `code` varchar(64) NOT NULL COMMENT '编号',
    `name` varchar(128) NOT NULL COMMENT '名称',
    `start_date` date NOT NULL COMMENT '生效开始日期',
    `end_date` date NOT NULL COMMENT '生效结束日期',
    `status` int NOT NULL DEFAULT 0 COMMENT '状态',
    `description` varchar(255) DEFAULT NULL COMMENT '备注',
    `tenant_id` varchar(32) NOT NULL COMMENT '租户ID',
    `create_by_id` varchar(32) DEFAULT NULL COMMENT '创建人ID',
    `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
    `update_by_id` varchar(32) DEFAULT NULL COMMENT '修改人ID',
    `update_time` datetime DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_quote_sheet_tenant_date` (`tenant_id`, `start_date`, `end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报价单';

CREATE TABLE `tbl_quote_sheet_detail` (
    `id` varchar(32) NOT NULL COMMENT 'ID',
    `quote_sheet_id` varchar(32) NOT NULL COMMENT '报价单ID',
    `product_id` varchar(32) NOT NULL COMMENT '商品ID',
    `sale_price` decimal(18,2) NOT NULL COMMENT '销售单价',
    `tenant_id` varchar(32) NOT NULL COMMENT '租户ID',
    `create_by_id` varchar(32) DEFAULT NULL COMMENT '创建人ID',
    `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
    `update_by_id` varchar(32) DEFAULT NULL COMMENT '修改人ID',
    `update_time` datetime DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quote_sheet_detail_sheet_product` (`quote_sheet_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报价单明细';

ALTER TABLE `tbl_sale_out_sheet_detail`
    ADD COLUMN `quote_sheet_id` varchar(32) NULL DEFAULT NULL COMMENT '报价单ID' AFTER `sale_order_detail_id`;
