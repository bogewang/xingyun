-- 报价单管理菜单及操作权限，挂载到“商品中心”。
INSERT INTO sys_menu (id, code, name, title, icon, component_type, component, request_param,
  parent_id, sys_module_id, path, no_cache, display, hidden, permission, is_special, available,
  description, create_by, create_by_id, create_time, update_by, update_by_id, update_time)
SELECT 'base-data-quote-menu', 'base-data-quote-menu', 'QuoteSheet', '报价单管理', 'ant-design:tags-outlined', 1,
  'base-data/quote/index', NULL, parent.id, parent.sys_module_id, '/base-data/quote', 0, 1, 0,
  'base-data:quote:query', 0, 1, '', '系统管理员', '1', NOW(), '系统管理员', '1', NOW()
FROM sys_menu parent
WHERE parent.title = '商品中心'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 'base-data-quote-menu');

INSERT INTO sys_menu (id, code, name, title, icon, component_type, component, request_param,
  parent_id, sys_module_id, path, no_cache, display, hidden, permission, is_special, available,
  description, create_by, create_by_id, create_time, update_by, update_by_id, update_time)
SELECT 'base-data-quote-add', 'base-data-quote-add', '', '新增报价单', NULL, 0, '', NULL,
  'base-data-quote-menu', menu.sys_module_id, '', 0, 2, 0, 'base-data:quote:add', 1, 1, '',
  '系统管理员', '1', NOW(), '系统管理员', '1', NOW()
FROM sys_menu menu WHERE menu.id = 'base-data-quote-menu'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 'base-data-quote-add');

INSERT INTO sys_menu (id, code, name, title, icon, component_type, component, request_param,
  parent_id, sys_module_id, path, no_cache, display, hidden, permission, is_special, available,
  description, create_by, create_by_id, create_time, update_by, update_by_id, update_time)
SELECT 'base-data-quote-modify', 'base-data-quote-modify', '', '修改报价单', NULL, 0, '', NULL,
  'base-data-quote-menu', menu.sys_module_id, '', 0, 2, 0, 'base-data:quote:modify', 1, 1, '',
  '系统管理员', '1', NOW(), '系统管理员', '1', NOW()
FROM sys_menu menu WHERE menu.id = 'base-data-quote-menu'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 'base-data-quote-modify');

INSERT INTO sys_menu (id, code, name, title, icon, component_type, component, request_param,
  parent_id, sys_module_id, path, no_cache, display, hidden, permission, is_special, available,
  description, create_by, create_by_id, create_time, update_by, update_by_id, update_time)
SELECT 'base-data-quote-delete', 'base-data-quote-delete', '', '删除报价单', NULL, 0, '', NULL,
  'base-data-quote-menu', menu.sys_module_id, '', 0, 2, 0, 'base-data:quote:delete', 1, 1, '',
  '系统管理员', '1', NOW(), '系统管理员', '1', NOW()
FROM sys_menu menu WHERE menu.id = 'base-data-quote-menu'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 'base-data-quote-delete');
