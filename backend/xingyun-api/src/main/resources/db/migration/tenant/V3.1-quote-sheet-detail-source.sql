ALTER TABLE `tbl_quote_sheet_detail`
    ADD COLUMN `product_snapshot` text NULL COMMENT '商品快照（JSON）' AFTER `product_id`;

ALTER TABLE `tbl_sale_out_sheet_detail`
    ADD COLUMN `source_id` varchar(32) NULL DEFAULT NULL COMMENT '来源报价明细ID' AFTER `product_id`;

ALTER TABLE `tbl_receive_sheet_detail`
    ADD COLUMN `source_id` varchar(32) NULL DEFAULT NULL COMMENT '来源报价明细ID' AFTER `product_id`;
