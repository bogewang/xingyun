INSERT IGNORE INTO sys_parameter (pm_key, pm_value, description, create_by, create_by_id, create_time,
                                  update_by, update_by_id, update_time)
VALUES ('sale_out_category_detail_export_enabled', 'true', '是否启用销售出库分类汇总导出按钮', '系统管理员', '1', NOW(), '系统管理员', '1', NOW());
