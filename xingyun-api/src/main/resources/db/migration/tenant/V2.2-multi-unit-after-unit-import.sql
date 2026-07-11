-- 第二部分：部署项目并通过“单位管理”导入单位后执行。
-- 将旧商品单位名称转换为单位字典 ID。
UPDATE `base_data_product` SET `unit` = '板' WHERE `unit` = '版';

UPDATE `base_data_product` p
    INNER JOIN `base_data_unit` u ON u.`name` = p.`unit` AND u.`available` = 1
SET p.`unit` = u.`id`
WHERE p.`unit` IS NOT NULL AND p.`unit` <> '' AND p.`unit` <> u.`id`;

-- 商品的主单位关系；库存与原单据数量均以该主单位核算。
INSERT INTO `base_data_product_unit`
(`id`, `product_id`, `unit_name`, `conversion_rate`, `base_unit`, `available`, `sort_no`, `create_time`, `update_time`)
SELECT CONCAT('UNIT', p.id), p.id, u.name, 1, 1, 1, 0, NOW(), NOW()
FROM `base_data_product` p
    INNER JOIN `base_data_unit` u ON u.id = p.unit AND u.available = 1
WHERE NOT EXISTS (
    SELECT 1 FROM `base_data_product_unit` pu
    WHERE pu.product_id = p.id AND pu.unit_name = u.name);

-- 补齐历史单据交易单位快照：优先最大可整除辅单位，未匹配时回填主单位。
UPDATE `tbl_receive_sheet_detail` d
    INNER JOIN `base_data_product_unit` pu ON pu.product_id = d.product_id
        AND pu.base_unit = 0 AND pu.available = 1 AND MOD(d.order_num, pu.conversion_rate) = 0
        AND pu.conversion_rate = (SELECT MAX(pu2.conversion_rate) FROM `base_data_product_unit` pu2
                                  WHERE pu2.product_id = d.product_id AND pu2.base_unit = 0 AND pu2.available = 1
                                    AND MOD(d.order_num, pu2.conversion_rate) = 0)
SET d.unit_id = pu.id, d.unit_name = pu.unit_name, d.conversion_rate = pu.conversion_rate,
    d.business_num = d.order_num / pu.conversion_rate
WHERE d.unit_id IS NULL OR d.unit_name IS NULL OR d.conversion_rate IS NULL OR d.business_num IS NULL;

UPDATE `tbl_sale_out_sheet_detail` d
    INNER JOIN `base_data_product_unit` pu ON pu.product_id = d.product_id
        AND pu.base_unit = 0 AND pu.available = 1 AND MOD(d.order_num, pu.conversion_rate) = 0
        AND pu.conversion_rate = (SELECT MAX(pu2.conversion_rate) FROM `base_data_product_unit` pu2
                                  WHERE pu2.product_id = d.product_id AND pu2.base_unit = 0 AND pu2.available = 1
                                    AND MOD(d.order_num, pu2.conversion_rate) = 0)
SET d.unit_id = pu.id, d.unit_name = pu.unit_name, d.conversion_rate = pu.conversion_rate,
    d.business_num = d.order_num / pu.conversion_rate
WHERE d.unit_id IS NULL OR d.unit_name IS NULL OR d.conversion_rate IS NULL OR d.business_num IS NULL;

UPDATE `tbl_receive_sheet_detail` d
    INNER JOIN `base_data_product_unit` pu ON pu.product_id = d.product_id AND pu.base_unit = 1
SET d.unit_id = pu.id, d.unit_name = pu.unit_name, d.conversion_rate = 1, d.business_num = d.order_num
WHERE d.unit_id IS NULL OR d.unit_name IS NULL OR d.conversion_rate IS NULL OR d.business_num IS NULL;

UPDATE `tbl_sale_out_sheet_detail` d
    INNER JOIN `base_data_product_unit` pu ON pu.product_id = d.product_id AND pu.base_unit = 1
SET d.unit_id = pu.id, d.unit_name = pu.unit_name, d.conversion_rate = 1, d.business_num = d.order_num
WHERE d.unit_id IS NULL OR d.unit_name IS NULL OR d.conversion_rate IS NULL OR d.business_num IS NULL;

-- 表头数量按交易单位数量汇总；库存仍以明细 order_num 主单位数量核算。
UPDATE `tbl_receive_sheet` s
    INNER JOIN (SELECT sheet_id, SUM(business_num) total_num FROM `tbl_receive_sheet_detail` GROUP BY sheet_id) d
        ON d.sheet_id = s.id
SET s.total_num = d.total_num;

UPDATE `tbl_sale_out_sheet` s
    INNER JOIN (SELECT sheet_id, SUM(business_num) total_num FROM `tbl_sale_out_sheet_detail` GROUP BY sheet_id) d
        ON d.sheet_id = s.id
SET s.total_num = d.total_num;

-- 核对未能转换单位的商品；有结果时请补齐单位字典后重跑第二部分。
SELECT p.id, p.code, p.name, p.unit
FROM `base_data_product` p
    LEFT JOIN `base_data_unit` u ON u.id = p.unit
WHERE p.unit IS NOT NULL AND p.unit <> '' AND u.id IS NULL;
