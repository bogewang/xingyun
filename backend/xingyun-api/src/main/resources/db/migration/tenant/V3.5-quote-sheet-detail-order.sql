-- 报价单明细排序号：保存时按页面商品顺序赋值，读取时按此顺序展示。
ALTER TABLE `tbl_quote_sheet_detail`
    ADD COLUMN `order_no` int NOT NULL DEFAULT 0 COMMENT '排序号' AFTER `inquiry_product`,
    ADD KEY `idx_quote_sheet_detail_order` (`quote_sheet_id`, `order_no`);
