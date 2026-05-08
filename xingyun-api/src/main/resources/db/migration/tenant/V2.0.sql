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
