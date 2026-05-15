DROP TABLE IF EXISTS `base_data_product_retail`;
DROP TABLE IF EXISTS `base_data_product_purchase`;
DROP TABLE IF EXISTS `base_data_product_sale`;

ALTER TABLE `base_data_product` ADD COLUMN  `sale_price` decimal(24,6) DEFAULT NULL COMMENT '销售价';
ALTER TABLE `base_data_product` ADD COLUMN  `purchase_price` decimal(24,6) DEFAULT NULL COMMENT '采购价';
ALTER TABLE `base_data_product` ADD COLUMN  `retail_price` decimal(24,6) DEFAULT NULL COMMENT '零售价';

update sys_menu set sys_module_id = 3 where code = '2000011';

-- 语言
ALTER TABLE `tbl_print_template` ADD COLUMN `lang` VARCHAR(20) NULL DEFAULT NULL COMMENT '语言';

-- 业务类型
ALTER TABLE `tbl_print_template` ADD COLUMN `biz_type` VARCHAR(20) NULL DEFAULT NULL COMMENT '业务类型';

-- 版本
ALTER TABLE `tbl_print_template` ADD COLUMN `version` VARCHAR(20) NULL DEFAULT NULL COMMENT '版本';
-- 订单日期
ALTER TABLE `tbl_sale_order` ADD COLUMN `order_date` date NULL DEFAULT NULL COMMENT '订单日期';
ALTER TABLE `tbl_purchase_order` ADD COLUMN `order_date` date NULL DEFAULT NULL COMMENT '订单日期';
ALTER TABLE `base_data_customer` ADD COLUMN `nick_name` VARCHAR(50) NULL DEFAULT NULL COMMENT '昵称';
ALTER TABLE `tbl_sale_out_sheet` ADD COLUMN `order_date` date NULL DEFAULT NULL COMMENT '订单日期';
ALTER TABLE `tbl_receive_sheet` ADD COLUMN `order_date` date NULL DEFAULT NULL COMMENT '订单日期';

-- 成本单价
ALTER TABLE `tbl_sale_out_sheet_detail` ADD COLUMN `cost_price` decimal(24,6) NULL DEFAULT '0.000000' COMMENT '成本单价';
ALTER TABLE `tbl_sale_out_sheet_detail` ADD COLUMN `total_profit` decimal(24,6) NULL DEFAULT '0.000000' COMMENT '总利润';
ALTER TABLE `tbl_sale_out_sheet` ADD COLUMN `total_cost` decimal(24,6) NULL DEFAULT '0.000000' COMMENT '成本单价';
ALTER TABLE `tbl_sale_out_sheet_detail` ADD COLUMN `total_profit` decimal(24,6) NULL DEFAULT '0.000000' COMMENT '总利润';
ALTER TABLE `tbl_sale_out_sheet` ADD COLUMN `total_profit` decimal(24,6) NULL DEFAULT '0.000000' COMMENT '总利润';
ALTER TABLE `tbl_sale_out_sheet` ADD COLUMN `cost_price` decimal(24,6)  NULL DEFAULT '0.000000' COMMENT '成本单价';

-- 仓库非必填
ALTER TABLE tbl_sale_out_sheet modify `sc_id` varchar(32) NULL COMMENT '仓库ID';
ALTER TABLE tbl_sale_order modify `sc_id` varchar(32) NULL COMMENT '仓库ID';
ALTER TABLE tbl_purchase_order modify `sc_id` varchar(32) NULL COMMENT '仓库ID';
ALTER TABLE tbl_receive_sheet modify `sc_id` varchar(32) NULL COMMENT '仓库ID';

alter table tbl_sale_out_sheet_detail modify discount_rate decimal(16,2) NULL COMMENT '折扣率（%）';
alter table base_data_supplier modify `mnemonic_code` varchar(20) NULL COMMENT '简码';

-- 未支付金额
ALTER TABLE `base_data_supplier` ADD COLUMN `unpaid_amount` decimal(24,6) NULL DEFAULT '0.000000' COMMENT '未支付金额';
ALTER TABLE `base_data_supplier` ADD COLUMN `paid_amount` decimal(24,6) NULL DEFAULT '0.000000' COMMENT '已支付金额';
ALTER TABLE `base_data_customer` ADD COLUMN `unpaid_amount` decimal(24,6) NULL DEFAULT '0.000000' COMMENT '未支付金额';
ALTER TABLE `base_data_customer` ADD COLUMN `paid_amount` decimal(24,6) NULL DEFAULT '0.000000' COMMENT '已支付金额';
ALTER TABLE `tbl_sale_out_sheet` ADD COLUMN `paid_amount` decimal(24,6) NULL DEFAULT '0.000000' COMMENT '已支付金额';
ALTER TABLE `tbl_receive_sheet` ADD COLUMN `paid_amount` decimal(24,6) NULL DEFAULT '0.000000' COMMENT '已支付金额';

ALTER TABLE `tbl_sale_out_sheet` ADD COLUMN `fill_all_cost` boolean NULL DEFAULT NULL COMMENT '是否录完所有成本';
ALTER TABLE `base_data_product` ADD COLUMN `alias` varchar(5000) NULL COMMENT '别名';
ALTER TABLE `base_data_product` ADD COLUMN `remark` varchar(255) NULL COMMENT '备注';
ALTER TABLE `base_data_product` ADD COLUMN `remark2` varchar(255) NULL COMMENT '备注二';

-- 0514
ALTER TABLE `tbl_sale_out_sheet_detail` ADD COLUMN `manual_input_cost` tinyint(1) NULL COMMENT '是否手动录入成本';
-- 0515
ALTER TABLE tbl_sale_out_sheet_detail modify `ori_price` decimal(24,6) NULL COMMENT '原价';
ALTER TABLE `base_data_product` ADD COLUMN `default_supplier` varchar(255) NULL COMMENT '默认供应商';


