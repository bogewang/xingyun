ALTER TABLE `tbl_sale_out_sheet`
  ADD COLUMN `confirm_num` decimal(24,6) NOT NULL DEFAULT '0.000000' COMMENT '验收数量',
  ADD COLUMN `confirm_amt` decimal(24,6) NOT NULL DEFAULT '0.000000' COMMENT '验收金额';

ALTER TABLE `tbl_sale_out_sheet_detail`
  ADD COLUMN `confirm_num` decimal(24,6) NOT NULL DEFAULT '0.000000' COMMENT '验收数量',
  ADD COLUMN `confirm_amt` decimal(24,6) NOT NULL DEFAULT '0.000000' COMMENT '验收金额';
