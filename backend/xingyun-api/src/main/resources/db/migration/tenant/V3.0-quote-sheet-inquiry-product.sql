ALTER TABLE `tbl_quote_sheet_detail`
    ADD COLUMN `inquiry_product` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否询价' AFTER `sale_price`;
