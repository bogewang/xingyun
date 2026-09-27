-- 项目报价规格、单位快照及采购收货单位字段调整
-- 适用：MySQL 5.7；基于本次提供的现有表结构，仅执行一次。
-- 在每个目标租户库执行，无须修改平台库。
-- 租户数据库迁移 V4.2，接续 V4.1。
-- 执行前备份；MySQL 5.7 的 ALTER TABLE 会隐式提交，不能依赖事务回滚。
-- 必须与应用适配同步发布：现有代码仍引用 display_name，改名后须改为 product_name。
-- 若此前已手动执行过对应 ALTER，请勿重复执行。

-- 1. 报价单项目归属沿用 tbl_quote_sheet.project_id。
-- 基础商品和商品单位表沿用现有结构，不新增项目商品表或换算表。
ALTER TABLE `tbl_quote_sheet_detail`
    CHANGE COLUMN `display_name` `product_name` varchar(128) NOT NULL
        COMMENT '报价单商品名称快照',
    ADD COLUMN `spec` varchar(128) DEFAULT NULL
        COMMENT '报价规格快照；空值表示本次报价无规格'
        AFTER `product_name`,
    ADD COLUMN `unit_id` varchar(32) DEFAULT NULL
        COMMENT '报价交易单位ID，关联商品单位配置'
        AFTER `spec`,
    ADD COLUMN `unit_name` varchar(64) DEFAULT NULL
        COMMENT '报价交易单位名称快照'
        AFTER `unit_id`,
    ADD COLUMN `conversion_rate` decimal(24,8) DEFAULT NULL
        COMMENT '1个报价交易单位对应的库存基础单位数量；历史未知时为空'
        AFTER `unit_name`;

-- 2. 历史规格优先取原商品快照：已有快照中的空规格必须保留。
-- 缺少规格快照时才采用当前主档；该回填不能恢复主档修改前的历史规格。
UPDATE `tbl_quote_sheet_detail` d
LEFT JOIN `base_data_product` p ON p.id = d.product_id
SET d.spec = CASE
    WHEN JSON_VALID(d.product_snapshot) THEN
        CASE
            WHEN JSON_CONTAINS_PATH(d.product_snapshot, 'one', '$.spec') THEN
                CASE WHEN JSON_TYPE(JSON_EXTRACT(d.product_snapshot, '$.spec')) = 'NULL'
                    THEN NULL
                    ELSE JSON_UNQUOTE(JSON_EXTRACT(d.product_snapshot, '$.spec'))
                END
            ELSE p.spec
        END
    ELSE p.spec
END;

-- 历史报价单位、换算率不自动回填：原报价可能按不同单位计价。
-- 核实报价计价单位后再设置 unit_id、unit_name、conversion_rate。
-- 不可将 base_data_product.unit 直接当作商品单位配置ID；需核实两类ID的关联。
-- 新报价由应用要求选择有效商品单位并保存大于0的换算率。
-- 同义展示单位“千克/公斤”对应同一基础重量时，换算率均可为1。

-- 3. 采购收货字段已存在，仅调整精度及注释。
-- 保留历史 NULL，避免把未知换算率当作1、未知交易数量当作0。
-- 本脚本不改写历史数量、金额、单价。
ALTER TABLE `tbl_receive_sheet_detail`
    MODIFY COLUMN `order_num` decimal(24,8) NOT NULL DEFAULT '0.00000000'
        COMMENT '基础单位收货数量，用于库存增减',
    MODIFY COLUMN `tax_price` decimal(24,6) NOT NULL DEFAULT '0.000000'
        COMMENT '交易单位采购价',
    MODIFY COLUMN `tax_amount` decimal(32,2) NOT NULL DEFAULT '0.00'
        COMMENT '采购总金额，按交易数量和交易单位采购价计算',
    MODIFY COLUMN `return_num` decimal(24,8) NOT NULL DEFAULT '0.00000000'
        COMMENT '已退货基础单位数量',
    MODIFY COLUMN `conversion_rate` decimal(24,8) DEFAULT NULL
        COMMENT '1个交易单位对应的库存基础单位数量；历史未知时为空',
    MODIFY COLUMN `business_num` decimal(24,8) DEFAULT NULL
        COMMENT '交易单位收货数量；历史未知时为空';

-- 核对数据，结果不为空时须逐项核实，不能直接改为 NOT NULL 或统一回填1。
SELECT id, sheet_id, product_id, business_num, conversion_rate, order_num
FROM tbl_receive_sheet_detail
WHERE conversion_rate IS NULL OR conversion_rate <= 0
   OR business_num IS NULL
   OR order_num <> ROUND(business_num * conversion_rate, 8);

-- 业务示例：基础单位袋，交易单位件，1件=20袋。
-- business_num=10，conversion_rate=20，order_num=200。
-- tax_price=100元/件，tax_amount=1000元；基础单位单价为5元/袋。
-- 新单据和退货必须由应用保存并沿用单位快照；仅执行DDL不会完成业务适配。
