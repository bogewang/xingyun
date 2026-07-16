ALTER TABLE `base_data_product`
    ADD COLUMN `inquiry_product` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否询价商品：0否，1是' AFTER `retail_price`;
