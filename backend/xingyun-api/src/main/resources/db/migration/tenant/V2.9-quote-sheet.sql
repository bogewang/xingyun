CREATE TABLE `tbl_quote_sheet` (
    `id` varchar(32) NOT NULL COMMENT 'ID',
    `name` varchar(128) NOT NULL COMMENT '名称',
    `start_date` date NOT NULL COMMENT '生效开始日期',
    `end_date` date NOT NULL COMMENT '生效结束日期',
    `status` int NOT NULL DEFAULT 0 COMMENT '状态',
    `description` varchar(255) DEFAULT NULL COMMENT '备注',
    `create_by_id` varchar(32) DEFAULT NULL COMMENT '创建人ID',
    `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
    `update_by_id` varchar(32) DEFAULT NULL COMMENT '修改人ID',
    `update_time` datetime DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_quote_sheet_date` (`start_date`, `end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报价单';

CREATE TABLE `tbl_quote_sheet_detail` (
    `id` varchar(32) NOT NULL COMMENT 'ID',
    `quote_sheet_id` varchar(32) NOT NULL COMMENT '报价单ID',
    `product_id` varchar(32) NOT NULL COMMENT '商品ID',
    `sale_price` decimal(18,2) NOT NULL COMMENT '销售单价',
    `create_by_id` varchar(32) DEFAULT NULL COMMENT '创建人ID',
    `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
    `update_by_id` varchar(32) DEFAULT NULL COMMENT '修改人ID',
    `update_time` datetime DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quote_sheet_detail_sheet_product` (`quote_sheet_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报价单明细';

ALTER TABLE `tbl_sale_out_sheet`
    ADD COLUMN `quote_sheet_id` varchar(32) NULL DEFAULT NULL COMMENT '报价单ID' AFTER `sale_order_id`;

-- 报价单管理菜单及操作权限，挂载到“商品中心”。
INSERT INTO sys_menu (id, code, name, title, icon, component_type, component, request_param,
                      parent_id, sys_module_id, path, no_cache, display, hidden, permission, is_special, available,
                      description, create_by, create_by_id, create_time, update_by, update_by_id, update_time)
SELECT 'base-data-quote-menu', 'quote-menu', 'QuoteSheet', '报价单管理', null, 1,
       'base-data/quote/index', NULL, parent.id, parent.sys_module_id, '/base-data/quote', 0, 1, 0,
       'base-data:quote:query', 0, 1, '', '系统管理员', '1', NOW(), '系统管理员', '1', NOW()
FROM sys_menu parent
WHERE parent.title = '商品中心'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 'base-data-quote-menu');

INSERT INTO sys_menu (id, code, name, title, icon, component_type, component, request_param,
                      parent_id, sys_module_id, path, no_cache, display, hidden, permission, is_special, available,
                      description, create_by, create_by_id, create_time, update_by, update_by_id, update_time)
SELECT 'base-data-quote-add', 'quote-add', '', '新增报价单', NULL, 0, '', NULL,
       'base-data-quote-menu', menu.sys_module_id, '', 0, 2, 0, 'base-data:quote:add', 1, 1, '',
       '系统管理员', '1', NOW(), '系统管理员', '1', NOW()
FROM sys_menu menu WHERE menu.id = 'base-data-quote-menu'
                     AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 'base-data-quote-add');

INSERT INTO sys_menu (id, code, name, title, icon, component_type, component, request_param,
                      parent_id, sys_module_id, path, no_cache, display, hidden, permission, is_special, available,
                      description, create_by, create_by_id, create_time, update_by, update_by_id, update_time)
SELECT 'base-data-quote-modify', 'quote-modify', '', '修改报价单', NULL, 0, '', NULL,
       'base-data-quote-menu', menu.sys_module_id, '', 0, 2, 0, 'base-data:quote:modify', 1, 1, '',
       '系统管理员', '1', NOW(), '系统管理员', '1', NOW()
FROM sys_menu menu WHERE menu.id = 'base-data-quote-menu'
                     AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 'base-data-quote-modify');

INSERT INTO sys_menu (id, code, name, title, icon, component_type, component, request_param,
                      parent_id, sys_module_id, path, no_cache, display, hidden, permission, is_special, available,
                      description, create_by, create_by_id, create_time, update_by, update_by_id, update_time)
SELECT 'base-data-quote-delete', 'quote-delete', '', '删除报价单', NULL, 0, '', NULL,
       'base-data-quote-menu', menu.sys_module_id, '', 0, 2, 0, 'base-data:quote:delete', 1, 1, '',
       '系统管理员', '1', NOW(), '系统管理员', '1', NOW()
FROM sys_menu menu WHERE menu.id = 'base-data-quote-menu'
                     AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 'base-data-quote-delete');


ALTER TABLE `tbl_quote_sheet_detail`
    ADD COLUMN `product_snapshot` text NULL COMMENT '商品快照（JSON）' AFTER `product_id`;

ALTER TABLE `tbl_sale_out_sheet_detail`
    ADD COLUMN `source_id` varchar(32) NULL DEFAULT NULL COMMENT '来源报价明细ID' AFTER `product_id`;

ALTER TABLE `tbl_receive_sheet_detail`
    ADD COLUMN `source_id` varchar(32) NULL DEFAULT NULL COMMENT '来源报价明细ID' AFTER `product_id`;

-- 报价单明细导出操作权限。
INSERT INTO sys_menu (id, code, name, title, icon, component_type, component, request_param,
                      parent_id, sys_module_id, path, no_cache, display, hidden, permission, is_special, available,
                      description, create_by, create_by_id, create_time, update_by, update_by_id, update_time)
SELECT 'base-data-quote-export', 'quote-export', '', '导出报价单明细', NULL, 0, '', NULL,
       'base-data-quote-menu', menu.sys_module_id, '', 0, 2, 0, 'base-data:quote:export', 1, 1, '',
       '系统管理员', '1', NOW(), '系统管理员', '1', NOW()
FROM sys_menu menu WHERE menu.id = 'base-data-quote-menu'
                     AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 'base-data-quote-export');

ALTER TABLE `tbl_quote_sheet_detail`
    ADD COLUMN `inquiry_product` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否询价' AFTER `sale_price`;

update sys_menu set sys_module_id = '3' where code in ('2000011001','2000011002')
-- 初始化报价单
-- 本脚本仅在不存在与 2026-01-01 至 2026-09-30 重叠的启用报价单时创建默认报价单。
SET @quote_sheet_id = REPLACE(UUID(), '-', '');

START TRANSACTION;

INSERT INTO tbl_quote_sheet (
    id, name, start_date, end_date, status, description,
    create_by_id, create_by, create_time, update_by_id, update_by, update_time
)
SELECT @quote_sheet_id, '默认报价单', '2026-01-01', '2026-09-30', 1,
       '根据现有启用商品及零售价自动生成',
       '1', '系统管理员', NOW(), '1', '系统管理员', NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_quote_sheet
    WHERE status = 1
      AND start_date <= '2026-09-30'
      AND end_date >= '2026-01-01'
);

INSERT INTO tbl_quote_sheet_detail (
    id, quote_sheet_id, product_id, product_snapshot, sale_price, inquiry_product,
    create_by_id, create_by, create_time, update_by_id, update_by, update_time
)
SELECT REPLACE(UUID(), '-', ''), @quote_sheet_id, p.id,
       JSON_OBJECT(
               'id', p.id,
               'code', p.code,
               'name', p.name,
               'shortName', p.short_name,
               'skuCode', p.sku_code,
               'externalCode', p.external_code,
               'categoryId', p.category_id,
               'brandId', p.brand_id,
               'taxRate', p.tax_rate,
               'saleTaxRate', p.sale_tax_rate,
               'purchasePrice', p.purchase_price,
               'retailPrice', p.retail_price,
               'spec', p.spec,
               'unit', p.unit,
               'weight', p.weight,
               'volume', p.volume,
               'available', p.available,
               'alias', p.alias,
               'defaultSupplier', p.default_supplier,
               'remark', p.remark,
               'remark2', p.remark2
       ),
       COALESCE(p.retail_price, 0),
       CASE WHEN p.retail_price IS NULL THEN 1 ELSE 0 END,
       '1', '系统管理员', NOW(), '1', '系统管理员', NOW()
FROM base_data_product p
         INNER JOIN tbl_quote_sheet q ON q.id = @quote_sheet_id
WHERE p.available = TRUE;

COMMIT;
