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
