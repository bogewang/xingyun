-- 采购收货单报价单来源：固化保存时匹配的报价单，避免明细查询按日期关联历史报价单产生重复行。
ALTER TABLE `tbl_receive_sheet`
    ADD COLUMN `quote_sheet_id` varchar(32) NULL DEFAULT NULL COMMENT '报价单ID' AFTER `purchase_order_id`;
