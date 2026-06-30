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
# ALTER TABLE tbl_product_stock modify `sc_id` varchar(32) NULL COMMENT '仓库ID';
# ALTER TABLE tbl_product_stock_log modify `sc_id` varchar(32) NULL COMMENT '仓库ID';

-- 0526 销售订单商品价格唯一配置
delete from sys_parameter where pm_key = 'product_sale_price_unique';
INSERT INTO `sys_parameter` (`id`, `pm_key`, `pm_value`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES (null, 'product_sale_price_unique', 'false', '销售订单商品价格唯一配置', '系统管理员', '1', now(), '系统管理员', '1', now());

-- 0528
update sys_menu set title = '结算汇总' where id = '4000006';

-- 对账结算
alter table settle_check_sheet add column biz_sheet_ids varchar(5000) null comment '业务单ID（逗号分隔）';
ALTER TABLE `settle_check_sheet` ADD COLUMN `biz_total_amount` decimal(32,2) NOT NULL DEFAULT 0.00 COMMENT '业务单汇总金额';

-- 实际日期
ALTER TABLE `tbl_sale_out_sheet_detail` ADD COLUMN `actual_date` date COMMENT '实际日期';
ALTER TABLE `tbl_receive_sheet_detail` ADD COLUMN `actual_date` date COMMENT '实际日期';
ALTER TABLE `tbl_sale_out_sheet_detail` ADD COLUMN `supplier_id` varchar(32) NULL COMMENT '供应商ID';

-- 对账结算
alter table settle_sheet add column biz_sheet_ids varchar(5000) null comment '业务单ID（逗号分隔）';
ALTER TABLE `settle_sheet` ADD COLUMN `total_un_settle_amt` decimal(32,2) COMMENT '未结算汇总金额';
ALTER TABLE `settle_sheet` ADD COLUMN `total_check_amt` decimal(32,2) COMMENT '对账金额';


--
ALTER TABLE settle_sheet modify start_date date COMMENT '起始日期';
ALTER TABLE settle_sheet modify end_date date COMMENT '截止日期';

ALTER TABLE settle_check_sheet modify start_date date COMMENT '起始日期';
ALTER TABLE settle_check_sheet modify end_date date COMMENT '截止日期';


-- 0625
delete from base_data_store_center where id = '2070120206814023680';
INSERT INTO `base_data_store_center` (`id`, `code`, `name`, `contact`, `telephone`, `city_id`, `address`, `people_num`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2070120206814023680', 'CK26062500001', '默认仓库', NULL, NULL, NULL, NULL, NULL, 1, '', '系统管理员', '1', '2026-06-25 20:21:33', '系统管理员', '1', '2026-06-25 20:21:33');
-- 仓库必填
UPDATE tbl_sale_out_sheet SET sc_id = '2070120206814023680';
UPDATE tbl_sale_order SET sc_id = '2070120206814023680';
UPDATE tbl_purchase_order SET sc_id = '2070120206814023680';
UPDATE tbl_receive_sheet SET sc_id = '2070120206814023680';

ALTER TABLE tbl_sale_out_sheet modify `sc_id` varchar(32) NOT NULL COMMENT '仓库ID';
ALTER TABLE tbl_sale_order modify `sc_id` varchar(32) NOT NULL COMMENT '仓库ID';
ALTER TABLE tbl_purchase_order modify `sc_id` varchar(32) NOT NULL COMMENT '仓库ID';
ALTER TABLE tbl_receive_sheet modify `sc_id` varchar(32) NOT NULL COMMENT '仓库ID';

ALTER TABLE `tbl_product_stock_log` ADD COLUMN `time_stamp` bigint(20) DEFAULT NULL COMMENT '时间戳';
ALTER TABLE `tbl_product_stock_log` modify `tax_amount` decimal(32,2) NULL COMMENT '含税金额';
ALTER TABLE `tbl_product_stock_log` modify   `cur_tax_price` decimal(24,6) NULL COMMENT '现含税成本价';
ALTER TABLE `tbl_product_stock_log` modify   `ori_tax_price` decimal(24,6) NULL COMMENT '原含税成本价';
ALTER TABLE `tbl_product_stock` modify   `tax_price` decimal(24,6) NULL COMMENT '含税价格';
ALTER TABLE `tbl_product_stock` modify   `tax_amount` decimal(32,2) NULL COMMENT '含税金额';


ALTER TABLE `tbl_sale_out_sheet_detail_lot`
    MODIFY `cost_tax_amount` decimal(24,2) NULL DEFAULT NULL COMMENT '含税成本金额',
    ADD COLUMN `settled_cost_num` decimal(24,6) NULL DEFAULT '0.000000' COMMENT '已回算数量',
    ADD COLUMN `cost_status` int NULL DEFAULT 2 COMMENT '成本状态';

ALTER TABLE `tbl_retail_out_sheet_detail_lot`
    MODIFY `cost_tax_amount` decimal(24,2) NULL DEFAULT NULL COMMENT '含税成本金额',
    ADD COLUMN `settled_cost_num` decimal(24,6) NULL DEFAULT '0.000000' COMMENT '已回算数量',
    ADD COLUMN `cost_status` int NULL DEFAULT 2 COMMENT '成本状态';

delete from sys_parameter where pm_key = 'sale_out_cost_price_use_stock_price';
INSERT INTO `sys_parameter` (`id`, `pm_key`, `pm_value`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES (null, 'sale_out_cost_price_use_stock_price', 'false', '销售出库刷新成本价时是否优先使用库存表成本价', '系统管理员', '1', now(), '系统管理员', '1', now());

DROP TABLE IF EXISTS `tbl_product_stock_pending_cost`;
CREATE TABLE `tbl_product_stock_pending_cost`
(
    `id`                 varchar(32)   NOT NULL COMMENT 'ID',
    `sc_id`              varchar(32)   NOT NULL COMMENT '仓库ID',
    `product_id`         varchar(32)   NOT NULL COMMENT '商品ID',
    `out_biz_id`         varchar(32)   DEFAULT NULL COMMENT '出库单据ID',
    `out_biz_detail_id`  varchar(32)   DEFAULT NULL COMMENT '出库单据明细ID',
    `out_biz_type`       int           NOT NULL COMMENT '出库业务类型',
    `lot_id`             varchar(32)   DEFAULT NULL COMMENT 'lot ID',
    `out_time`           datetime      NOT NULL COMMENT '出库时间',
    `pending_num`        decimal(24,6) NOT NULL COMMENT '待回算数量',
    `settled_num`        decimal(24,6) DEFAULT '0.000000' COMMENT '已回算数量',
    `settled_tax_amount` decimal(24,2) DEFAULT '0.00' COMMENT '已回算金额',
    `status`             int           NOT NULL COMMENT '状态',
    `available`          tinyint(1)    DEFAULT '1' COMMENT '是否有效',
    `description`        varchar(255)  DEFAULT NULL COMMENT '描述',
    `create_by`          varchar(50)   DEFAULT NULL COMMENT '创建人',
    `create_by_id`       varchar(32)   DEFAULT NULL COMMENT '创建人ID',
    `create_time`        datetime      DEFAULT NULL COMMENT '创建时间',
    `update_by`          varchar(50)   DEFAULT NULL COMMENT '修改人',
    `update_by_id`       varchar(32)   DEFAULT NULL COMMENT '修改人ID',
    `update_time`        datetime      DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`)
) COMMENT ='库存待回算成本';

DROP TABLE IF EXISTS `tbl_product_stock_pending_cost_settle`;
CREATE TABLE `tbl_product_stock_pending_cost_settle`
(
    `id`                varchar(32)   NOT NULL COMMENT 'ID',
    `pending_id`        varchar(32)   NOT NULL COMMENT '待回算记录ID',
    `in_biz_id`         varchar(32)   DEFAULT NULL COMMENT '入库单据ID',
    `in_biz_detail_id`  varchar(32)   DEFAULT NULL COMMENT '入库单据明细ID',
    `in_biz_type`       int           NOT NULL COMMENT '入库业务类型',
    `settle_num`        decimal(24,6) NOT NULL COMMENT '回算数量',
    `settle_tax_amount` decimal(24,2) NOT NULL COMMENT '回算金额',
    `available`         tinyint(1)    DEFAULT '1' COMMENT '是否有效',
    `description`       varchar(255)  DEFAULT NULL COMMENT '描述',
    `create_by`         varchar(50)   DEFAULT NULL COMMENT '创建人',
    `create_by_id`      varchar(32)   DEFAULT NULL COMMENT '创建人ID',
    `create_time`       datetime      DEFAULT NULL COMMENT '创建时间',
    `update_by`         varchar(50)   DEFAULT NULL COMMENT '修改人',
    `update_by_id`      varchar(32)   DEFAULT NULL COMMENT '修改人ID',
    `update_time`       datetime      DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`)
) COMMENT ='库存待回算成本回写明细';

