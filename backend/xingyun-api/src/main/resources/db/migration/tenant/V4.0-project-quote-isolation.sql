-- 租户内项目及报价单项目隔离。
CREATE TABLE `sys_project` (
    `id` varchar(32) NOT NULL COMMENT 'ID',
    `code` varchar(64) NOT NULL COMMENT '项目编码',
    `name` varchar(128) NOT NULL COMMENT '项目名称',
    `available` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `description` varchar(255) DEFAULT NULL COMMENT '备注',
    `create_by_id` varchar(32) DEFAULT NULL COMMENT '创建人ID',
    `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by_id` varchar(32) DEFAULT NULL COMMENT '修改人ID',
    `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
    `update_time` datetime DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目';

CREATE TABLE `sys_project_user` (
    `project_id` varchar(32) NOT NULL COMMENT '项目ID',
    `user_id` varchar(32) NOT NULL COMMENT '用户ID',
    PRIMARY KEY (`project_id`, `user_id`),
    KEY `idx_project_user_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员';

-- 历史报价单归入默认项目，保证升级后所有报价单均有明确归属。
INSERT INTO `sys_project` (`id`, `code`, `name`, `available`, `description`, `create_by_id`, `create_by`, `create_time`, `update_by_id`, `update_by`, `update_time`)
SELECT 'default-project', 'DEFAULT', '默认项目', 1, '系统升级自动创建', '1', '系统管理员', NOW(), '1', '系统管理员', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_project` WHERE `code` = 'DEFAULT');

ALTER TABLE `tbl_quote_sheet` ADD COLUMN `project_id` varchar(32) NULL COMMENT '项目ID' AFTER `id`;
UPDATE `tbl_quote_sheet` SET `project_id` = 'default-project' WHERE `project_id` IS NULL;
ALTER TABLE `tbl_quote_sheet`
    MODIFY COLUMN `project_id` varchar(32) NOT NULL COMMENT '项目ID',
    ADD KEY `idx_quote_sheet_project_date` (`project_id`, `start_date`, `end_date`);

ALTER TABLE `tbl_quote_sheet_detail` ADD COLUMN `display_name` varchar(128) NULL COMMENT '报价品名快照' AFTER `product_id`;
UPDATE `tbl_quote_sheet_detail` d INNER JOIN `base_data_product` p ON p.id = d.product_id
SET d.display_name = p.name WHERE d.display_name IS NULL;
ALTER TABLE `tbl_quote_sheet_detail` MODIFY COLUMN `display_name` varchar(128) NOT NULL COMMENT '报价品名快照';

-- 客户属于项目；供应商仍为租户共享数据。
ALTER TABLE `base_data_customer` ADD COLUMN `project_id` varchar(32) NULL COMMENT '项目ID' AFTER `id`;
UPDATE `base_data_customer` SET `project_id` = 'default-project' WHERE `project_id` IS NULL;
ALTER TABLE `base_data_customer`
    MODIFY COLUMN `project_id` varchar(32) NOT NULL COMMENT '项目ID',
    ADD KEY `idx_customer_project_available` (`project_id`, `available`);
