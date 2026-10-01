INSERT IGNORE INTO sys_parameter (pm_key, pm_value, description, create_by, create_by_id, create_time,
                                  update_by, update_by_id, update_time)
VALUES ('sale_out_order_summary_js_export_enabled', 'false', '是否显示销售出库订单汇总导出按钮（true显示，false隐藏）', '系统管理员', '1', NOW(), '系统管理员', '1', NOW());
