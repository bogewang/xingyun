-- 第一部分：部署前手工执行。只创建多单位所需结构，不初始化单位数据。
DROP TABLE IF EXISTS `base_data_product_unit`;
CREATE TABLE IF NOT EXISTS `base_data_product_unit`
(
    `id` varchar(32) NOT NULL COMMENT 'ID',
    `product_id` varchar(32) NOT NULL COMMENT '商品ID',
    `unit_name` varchar(20) NOT NULL COMMENT '单位名称',
    `conversion_rate` decimal(24,6) NOT NULL COMMENT '1个该单位对应主单位数量',
    `base_unit` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否主单位',
    `available` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
    `description` varchar(255) DEFAULT NULL COMMENT '描述',
    `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `create_by_id` varchar(32) DEFAULT NULL COMMENT '创建人ID',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `update_by_id` varchar(32) DEFAULT NULL COMMENT '更新人ID',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_unit_name` (`product_id`, `unit_name`, available)
) COMMENT ='商品多单位';

DROP TABLE IF EXISTS `base_data_unit`;
CREATE TABLE IF NOT EXISTS `base_data_unit`
(
    `id`          varchar(32)  NOT NULL,
    `code`        varchar(20)  NOT NULL,
    `name`        varchar(20)  NOT NULL,
    `available`   tinyint(1)   NOT NULL DEFAULT 1,
    `description` varchar(255) DEFAULT NULL,
    `create_by`   varchar(50)  DEFAULT NULL,
    `create_by_id` varchar(32) DEFAULT NULL,
    `create_time` datetime     DEFAULT NULL,
    `update_by`   varchar(50)  DEFAULT NULL,
    `update_by_id` varchar(32) DEFAULT NULL,
    `update_time` datetime     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_unit_code_available` (`code`, `available`),
    UNIQUE KEY `uk_unit_name_available` (`name`, `available`)
) COMMENT = '计量单位字典';


-- parent_id 与 sys_module_id 请按实际租户菜单结构调整。
DELETE FROM `sys_menu`
WHERE `id` IN ('base_data_unit', 'base_data_unit_add', 'base_data_unit_modify',
               'base_data_unit_delete', 'base_data_unit_import');

INSERT INTO `sys_menu`
(`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`)
VALUES
('base_data_unit', 'BDU001', 'Unit', '单位管理', NULL, 0, '/base-data/unit/index', NULL, '2001', '4', '/base-data/unit', 0, 1, 0, 'base-data:unit:query', 1, 1, '', '系统管理员', '1', NOW(), '系统管理员', '1', NOW()),
('base_data_unit_add', 'BDU001001', '', '新增单位', NULL, 0, '', NULL, 'base_data_unit', '4', '', 0, 2, 0, 'base-data:unit:add', 1, 1, '', '系统管理员', '1', NOW(), '系统管理员', '1', NOW()),
('base_data_unit_modify', 'BDU001002', '', '修改单位', NULL, 0, '', NULL, 'base_data_unit', '4', '', 0, 2, 0, 'base-data:unit:modify', 1, 1, '', '系统管理员', '1', NOW(), '系统管理员', '1', NOW()),
('base_data_unit_delete', 'BDU001003', '', '删除单位', NULL, 0, '', NULL, 'base_data_unit', '4', '', 0, 2, 0, 'base-data:unit:delete', 1, 1, '', '系统管理员', '1', NOW(), '系统管理员', '1', NOW()),
('base_data_unit_import', 'BDU001004', '', '导入单位', NULL, 0, '', NULL, 'base_data_unit', '4', '', 0, 2, 0, 'base-data:unit:import', 1, 1, '', '系统管理员', '1', NOW(), '系统管理员', '1', NOW());

ALTER TABLE `tbl_receive_sheet_detail`
    ADD COLUMN `unit_id` varchar(32) NULL COMMENT '交易单位ID',
    ADD COLUMN `unit_name` varchar(20) NULL COMMENT '交易单位快照',
    ADD COLUMN `conversion_rate` decimal(24,6) NULL COMMENT '换算率快照',
    ADD COLUMN `business_num` decimal(24,6) NULL COMMENT '交易单位数量';

ALTER TABLE `tbl_sale_out_sheet_detail`
    ADD COLUMN `unit_id` varchar(32) NULL COMMENT '交易单位ID',
    ADD COLUMN `unit_name` varchar(20) NULL COMMENT '交易单位快照',
    ADD COLUMN `conversion_rate` decimal(24,6) NULL COMMENT '换算率快照',
    ADD COLUMN `business_num` decimal(24,6) NULL COMMENT '交易单位数量';
