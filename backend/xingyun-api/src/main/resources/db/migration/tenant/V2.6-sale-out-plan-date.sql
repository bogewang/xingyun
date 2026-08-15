ALTER TABLE `tbl_sale_out_sheet_detail`
    ADD COLUMN `plan_date` date NULL DEFAULT NULL COMMENT '计划日期' AFTER `actual_date`;
