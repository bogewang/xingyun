ALTER TABLE `tbl_receive_sheet_detail`
    ADD COLUMN `production_date` varchar(10) NULL DEFAULT NULL COMMENT '生产日期' AFTER `actual_date`;
