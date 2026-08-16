ALTER TABLE `tbl_sale_out_sheet`
    ADD COLUMN `delivered` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已送货' AFTER `fill_all_cost`;
