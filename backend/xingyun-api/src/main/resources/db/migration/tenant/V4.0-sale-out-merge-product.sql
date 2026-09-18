INSERT IGNORE INTO sys_parameter (pm_key, pm_value, description, create_by, create_by_id, create_time,
                                  update_by, update_by_id, update_time)
VALUES ('sale_out_merge_product_enabled', 'false', '是否启用销售出库合并商品功能', '系统管理员', '1', NOW(), '系统管理员', '1', NOW());
