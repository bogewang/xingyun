ALTER TABLE `tbl_sale_out_sheet`
  ADD COLUMN `settle_version` BIGINT NOT NULL DEFAULT 0 COMMENT '结算版本号';

ALTER TABLE `tbl_sale_return`
  ADD COLUMN `settle_version` BIGINT NOT NULL DEFAULT 0 COMMENT '结算版本号';
