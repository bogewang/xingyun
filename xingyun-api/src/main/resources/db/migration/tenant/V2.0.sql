DROP TABLE IF EXISTS `base_data_product_retail`;
DROP TABLE IF EXISTS `base_data_product_purchase`;
DROP TABLE IF EXISTS `base_data_product_sale`;

update sys_menu set sys_module_id = 3 where code = '2000011';

-- 语言
ALTER TABLE `tbl_print_template` ADD COLUMN `lang` VARCHAR(20) NULL DEFAULT NULL COMMENT '语言';

-- 业务类型
ALTER TABLE `tbl_print_template` ADD COLUMN `biz_type` VARCHAR(20) NULL DEFAULT NULL COMMENT '业务类型';

-- 版本
ALTER TABLE `tbl_print_template` ADD COLUMN `version` VARCHAR(20) NULL DEFAULT NULL COMMENT '版本';

ALTER TABLE `tbl_sale_order` ADD COLUMN `order_date` date NULL DEFAULT NULL COMMENT '订单日期';
ALTER TABLE `tbl_purchase_order` ADD COLUMN `order_date` date NULL DEFAULT NULL COMMENT '订单日期';

ALTER TABLE `base_data_customer` ADD COLUMN `nick_name` VARCHAR(50) NULL DEFAULT NULL COMMENT '昵称';

ALTER TABLE `tbl_sale_out_sheet` ADD COLUMN `order_date` date NULL DEFAULT NULL COMMENT '订单日期';
ALTER TABLE `tbl_receive_sheet` ADD COLUMN `order_date` date NULL DEFAULT NULL COMMENT '订单日期';

-- 成本单价
ALTER TABLE `tbl_sale_out_sheet` ADD COLUMN `cost_price` decimal(24,6) NOT NULL DEFAULT '0.000000' COMMENT '成本单价';
ALTER TABLE `tbl_sale_out_sheet` ADD COLUMN `total_profit` decimal(24,6) NOT NULL DEFAULT '0.000000' COMMENT '总利润';

-- 仓库非必填
ALTER TABLE tbl_sale_out_sheet modify `sc_id` varchar(32) NULL COMMENT '仓库ID';
ALTER TABLE tbl_sale_order modify `sc_id` varchar(32) NULL COMMENT '仓库ID';
ALTER TABLE tbl_purchase_order modify `sc_id` varchar(32) NULL COMMENT '仓库ID';
ALTER TABLE tbl_receive_sheet modify `sc_id` varchar(32) NULL COMMENT '仓库ID';

alter table tbl_sale_out_sheet_detail modify discount_rate decimal(16,2) NULL COMMENT '折扣率（%）';


alter table base_data_supplier modify `mnemonic_code` varchar(20) NULL COMMENT '简码';


ALTER TABLE `base_data_supplier` ADD COLUMN `unpaid_amount` decimal(24,6) NULL DEFAULT '0.000000' COMMENT '未支付金额';
ALTER TABLE `base_data_supplier` ADD COLUMN `paid_amount` decimal(24,6) NULL DEFAULT '0.000000' COMMENT '已支付金额';
