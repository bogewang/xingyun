ALTER TABLE `tbl_print_template`
  ADD COLUMN `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认模板（同一业务类型下仅允许一个默认）';
