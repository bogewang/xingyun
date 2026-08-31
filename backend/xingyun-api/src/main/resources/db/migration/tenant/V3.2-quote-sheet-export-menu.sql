-- 报价单明细导出操作权限。
INSERT INTO sys_menu (id, code, name, title, icon, component_type, component, request_param,
  parent_id, sys_module_id, path, no_cache, display, hidden, permission, is_special, available,
  description, create_by, create_by_id, create_time, update_by, update_by_id, update_time)
SELECT 'base-data-quote-export', 'quote-export', '', '导出报价单明细', NULL, 0, '', NULL,
  'base-data-quote-menu', menu.sys_module_id, '', 0, 2, 0, 'base-data:quote:export', 1, 1, '',
  '系统管理员', '1', NOW(), '系统管理员', '1', NOW()
FROM sys_menu menu WHERE menu.id = 'base-data-quote-menu'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 'base-data-quote-export');
