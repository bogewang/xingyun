-- 采购收货单乐观锁版本号。
ALTER TABLE `tbl_receive_sheet`
    ADD COLUMN `version` int NOT NULL DEFAULT 0 COMMENT '版本号' AFTER `id`;
