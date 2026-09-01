-- 补齐打印模板菜单及操作权限。
INSERT INTO sys_menu (id, code, name, title, icon, component_type, component, request_param,
                      parent_id, sys_module_id, path, no_cache, display, hidden, permission, is_special, available,
                      description, create_by, create_by_id, create_time, update_by, update_by_id, update_time)
SELECT '2000011', '2000011', 'PrintTemplate', '打印模板', NULL, 0, '/base-data/print-template/index', NULL,
       parent.id, 3, '/print-template', 0, 1, 0, 'base-data:print-template:query', 1, 1, '',
       '系统管理员', '1', NOW(), '系统管理员', '1', NOW()
FROM sys_menu parent
WHERE parent.id = '2000'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'base-data:print-template:query');

INSERT INTO sys_menu (id, code, name, title, icon, component_type, component, request_param,
                      parent_id, sys_module_id, path, no_cache, display, hidden, permission, is_special, available,
                      description, create_by, create_by_id, create_time, update_by, update_by_id, update_time)
SELECT '2000011001', '2000011001', '', '新增打印模板', NULL, 0, '', NULL,
       menu.id, 3, '', 0, 2, 0, 'base-data:print-template:add', 1, 1, '',
       '系统管理员', '1', NOW(), '系统管理员', '1', NOW()
FROM sys_menu menu
WHERE menu.permission = 'base-data:print-template:query'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'base-data:print-template:add');

INSERT INTO sys_menu (id, code, name, title, icon, component_type, component, request_param,
                      parent_id, sys_module_id, path, no_cache, display, hidden, permission, is_special, available,
                      description, create_by, create_by_id, create_time, update_by, update_by_id, update_time)
SELECT '2000011002', '2000011002', '', '修改打印模板', NULL, 0, '', NULL,
       menu.id, 3, '', 0, 2, 0, 'base-data:print-template:modify', 1, 1, '',
       '系统管理员', '1', NOW(), '系统管理员', '1', NOW()
FROM sys_menu menu
WHERE menu.permission = 'base-data:print-template:query'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'base-data:print-template:modify');

INSERT INTO sys_menu (id, code, name, title, icon, component_type, component, request_param,
                      parent_id, sys_module_id, path, no_cache, display, hidden, permission, is_special, available,
                      description, create_by, create_by_id, create_time, update_by, update_by_id, update_time)
SELECT '2000011003', '2000011003', '', '删除打印模板', NULL, 0, '', NULL,
       menu.id, 3, '', 0, 2, 0, 'base-data:print-template:delete', 1, 1, '',
       '系统管理员', '1', NOW(), '系统管理员', '1', NOW()
FROM sys_menu menu
WHERE menu.permission = 'base-data:print-template:query'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'base-data:print-template:delete');
