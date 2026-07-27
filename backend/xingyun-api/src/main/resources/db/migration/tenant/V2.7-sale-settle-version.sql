ALTER TABLE `tbl_sale_out_sheet`
  ADD COLUMN `settle_version` BIGINT NOT NULL DEFAULT 0 COMMENT '结算版本号';

ALTER TABLE `tbl_sale_return`
  ADD COLUMN `settle_version` BIGINT NOT NULL DEFAULT 0 COMMENT '结算版本号';


update sys_menu set parent_id = '4000', title = '供应商结算',
  component = '/settle/sheet/index', path = '/supplier/sheet' where id = '4000006';
update sys_menu set available = '0',title = '供应商结算(已废弃)' where id = '4000007';


update sys_menu set parent_id = '4000', title = '客户结算',
  component = '/customer-settle/sheet/index', path = '/customer/sheet' where id = '4000012';
update sys_menu set available = '0',title = '客户结算(已废弃)' where id = '4000008';

# 初始化结算状态
update tbl_sale_out_sheet set settle_status = 7 where settle_status = 0;

ALTER TABLE customer_settle_check_sheet modify start_date date COMMENT '起始日期';
ALTER TABLE customer_settle_check_sheet modify end_date date COMMENT '截止日期';