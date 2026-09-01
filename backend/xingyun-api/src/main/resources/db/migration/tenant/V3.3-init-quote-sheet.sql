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
       COALESCE(p.sale_price, 0),
       p.inquiry_product,
       '1', '系统管理员', NOW(), '1', '系统管理员', NOW()
FROM base_data_product p
         INNER JOIN tbl_quote_sheet q ON q.id = @quote_sheet_id
WHERE p.available = TRUE;

COMMIT;
