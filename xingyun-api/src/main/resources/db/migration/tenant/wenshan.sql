delete from sys_menu where id = '2003003006';
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`,
                        `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`,
                        `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`,
                        `update_by`, `update_by_id`, `update_time`)
VALUES ('2003003006', '2003003006', '', '文山销售出库单导出', NULL, 0, '', NULL, '2003003', '6', '', 0, 2, 0,
        'wenshan:sale:out:saleexport', 1, 1, '', '系统管理员', '1', now(), '系统管理员', '1',
        now());
