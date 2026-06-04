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
ALTER TABLE `tbl_sale_out_sheet` ADD COLUMN `total_profit` decimal(24,6) NULL DEFAULT '0.000000' COMMENT '总利润';

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

-- 0516
delete from sys_menu where id = '2003003007';
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2003003007', '2003003007', '', '销售单查看利润', NULL, 0, '', NULL, '2003003', '6', '', 0, 2, 0, 'sale:out:profit', 1, 1, '', '系统管理员', '1', now(), '系统管理员', '1', now());

delete from base_data_product_category;
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629760', 'SPFL1001', '乳制品', NULL, 1, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 10:01:32');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629761', 'SPFL1002', '日用百货', NULL, 1, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 10:01:32');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629762', 'SPFL1003', '豆类', NULL, 1, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 10:01:32');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629763', 'SPFL1004', '猪肉', NULL, 0, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 20:02:35');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629764', 'SPFL1005', '鸡鸭肉', NULL, 0, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 20:02:35');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629765', 'SPFL1006', '羊肉', NULL, 0, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 19:33:42');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629766', 'SPFL1007', '牛肉', NULL, 0, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 20:02:35');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629767', 'SPFL1008', '蛋类', NULL, 1, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 10:01:32');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629768', 'SPFL1009', '蔬菜', NULL, 1, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 10:01:32');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629769', 'SPFL1010', '水果', NULL, 1, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 10:01:32');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629770', 'SPFL1011', '面制品', NULL, 1, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 10:01:32');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629771', 'SPFL1012', '酱菜', NULL, 1, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 10:01:32');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629772', 'SPFL1013', '米线', NULL, 1, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 10:01:32');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629773', 'SPFL1014', '水产', NULL, 1, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 10:01:32');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629774', 'SPFL1015', '饮料', NULL, 1, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 10:01:32');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629775', 'SPFL1016', '熟食类', NULL, 1, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 10:01:32');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629776', 'SPFL1017', '调料干杂', NULL, 1, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 10:01:32');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629777', 'SPFL1018', '肉类', NULL, 1, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 10:01:32');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629778', 'SPFL1019', '糕点', NULL, 1, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 10:01:32');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629779', 'SPFL1020', '粮油', NULL, 1, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 10:01:32');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055106271056629780', 'SPFL1021', '牛奶', NULL, 1, '', '王波', '2055100001188712448', '2026-05-15 10:01:32', '王波', '2055100001188712448', '2026-05-15 10:01:32');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055256993538641920', 'SPFL1022', '海鲜类', '2055106271056629777', 0, '', '王波', '2055100001188712448', '2026-05-15 20:00:27', '王波', '2055100001188712448', '2026-05-15 20:03:53');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055257698437566464', 'SPFL1023', '猪肉', '2055106271056629777', 1, '', '王波', '2055100001188712448', '2026-05-15 20:03:15', '王波', '2055100001188712448', '2026-05-15 20:03:15');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055257804960305152', 'SPFL1024', '牛肉', '2055106271056629777', 1, '', '王波', '2055100001188712448', '2026-05-15 20:03:40', '王波', '2055100001188712448', '2026-05-15 20:03:40');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055258065732767744', 'SPFL1025', '鸡鸭肉', '2055106271056629777', 1, '', '王波', '2055100001188712448', '2026-05-15 20:04:42', '王波', '2055100001188712448', '2026-05-15 20:04:42');
INSERT INTO `base_data_product_category` (`id`, `code`, `name`, `parent_id`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2055258065732767745', 'SPFL1026', '冻品', null, 1, '', '王波', '2055100001188712448', now(), '王波', '2055100001188712448', now());

-- 0519 销售利润（按单据）
delete from sys_menu where id in ('6000', '6000001', '6000001001');
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('6000', '6000', 'Report', '报表', 'ant-design:bar-chart-outlined', NULL, '', NULL, NULL, '17', '/report', 0, 0, 0, '', 1, 1, '', '系统管理员', '1', now(), '系统管理员', '1', now());
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('6000001', '6000001', 'SaleProfitSheetReport', '销售利润（按单据）', NULL, 0, '/report/sale-profit/sheet', NULL, '6000', '17', '/sale-profit/sheet', 0, 1, 0, 'report:sale-profit:query', 1, 1, '', '系统管理员', '1', now(), '系统管理员', '1', now());
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('6000001001', '6000001001', '', '导出销售利润（按单据）', NULL, 0, '', NULL, '6000001', '17', '', 0, 2, 0, 'report:sale-profit:export', 1, 1, '', '系统管理员', '1', now(), '系统管理员', '1', now());


-- 051902 销售利润（按商品）
delete from sys_menu where id in ('6000002', '6000002001');
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('6000002', '6000002', 'SaleProfitProductReport', '销售利润（按商品）', NULL, 0, '/report/sale-profit/product', NULL, '6000', '17', '/sale-profit/product', 0, 1, 0, 'report:sale-profit:product:query', 1, 1, '', '系统管理员', '1', now(), '系统管理员', '1', now());
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('6000002001', '6000002001', '', '导出销售利润（按商品）', NULL, 0, '', NULL, '6000002', '17', '', 0, 2, 0, 'report:sale-profit:product:export', 1, 1, '', '系统管理员', '1', now(), '系统管理员', '1', now());

-- 0521 销售趋势分析
delete from sys_menu where id = '6000003';
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('6000003', '6000003', 'SaleTrendReport', '销售趋势分析', NULL, 0, '/report/sale-trend/index', NULL, '6000', '17', '/sale-trend', 0, 1, 0, 'report:sale-trend:query', 1, 1, '', '系统管理员', '1', now(), '系统管理员', '1', now());


-- 0523 系统参数
delete from sys_parameter where pm_key = 'latest_price_override_product_price';
INSERT INTO `sys_parameter` (`id`, `pm_key`, `pm_value`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES (null, 'latest_price_override_product_price', 'false', '最新价格是否覆盖商品价格', '系统管理员', '1', now(), '系统管理员', '1', now());

-- 0525 仓库非必填
ALTER TABLE tbl_product_stock modify `sc_id` varchar(32) NULL COMMENT '仓库ID';
ALTER TABLE tbl_product_stock_log modify `sc_id` varchar(32) NULL COMMENT '仓库ID';

-- 0526 销售订单商品价格唯一配置
delete from sys_parameter where pm_key = 'product_sale_price_unique';
INSERT INTO `sys_parameter` (`id`, `pm_key`, `pm_value`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES (null, 'product_sale_price_unique', 'false', '销售订单商品价格唯一配置', '系统管理员', '1', now(), '系统管理员', '1', now());


-- 实际日期
ALTER TABLE `tbl_sale_out_sheet_detail` ADD COLUMN `actual_date` date COMMENT '实际日期';
ALTER TABLE `tbl_receive_sheet_detail` ADD COLUMN `actual_date` date COMMENT '实际日期';
ALTER TABLE `tbl_sale_out_sheet_detail` ADD COLUMN `supplier_id` varchar(32) NULL COMMENT '供应商ID';
