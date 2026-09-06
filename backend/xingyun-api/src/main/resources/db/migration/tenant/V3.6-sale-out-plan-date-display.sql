INSERT IGNORE INTO `sys_parameter` (`pm_key`, `pm_value`, `description`, `create_by`, `create_by_id`, `create_time`,
                                    `update_by`, `update_by_id`, `update_time`)
VALUES ('sale_out_show_plan_date', 'true', '销售出库单是否展示计划日期', '系统管理员', '1', NOW(), '系统管理员', '1', NOW());
