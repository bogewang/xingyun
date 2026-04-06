package com.lframework.xingyun.basedata.bo.product.info;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.annotations.convert.EnumConvert;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.entity.*;
import com.lframework.xingyun.basedata.service.product.ProductBrandService;
import com.lframework.xingyun.basedata.service.product.ProductCategoryService;
import com.lframework.xingyun.basedata.service.product.ProductPurchaseService;
import com.lframework.xingyun.basedata.service.product.ProductRetailService;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QueryProductBo extends BaseBo<Product> {

  /**
   * ID
   */
  @ApiModelProperty("ID")
  private String id;

  /**
   * 编号
   */
  @ApiModelProperty("编号")
  private String code;

  /**
   * 名称
   */
  @ApiModelProperty("名称")
  private String name;

  /**
   * 名称
   */
  @ApiModelProperty("简称")
  private String shortName;

  /**
   * SKU
   */
  @ApiModelProperty("SKU")
  private String skuCode;

  /**
   * 单位
   */
  @ApiModelProperty("单位")
  private String unit;

  /**
   * 规格
   */
  @ApiModelProperty("规格")
  private String spec;

  /**
   * 采购价
   */
  @ApiModelProperty("采购价")
  private BigDecimal purchasePrice;

  /**
   * 零售价
   */
  @ApiModelProperty("零售价")
  private BigDecimal retailPrice;

  /**
   * 分类名称
   */
  @ApiModelProperty("分类名称")
  private String categoryName;

  /**
   * 品牌名称
   */
  @ApiModelProperty("品牌名称")
  private String brandName;

  /**
   * 商品类型
   */
  @ApiModelProperty("商品类型")
  @EnumConvert
  private Integer productType;

  /**
   * 创建时间
   */
  @ApiModelProperty("创建时间")
  @JsonFormat(pattern = StringPool.DATE_TIME_PATTERN)
  private LocalDateTime createTime;

  /**
   * 修改时间
   */
  @ApiModelProperty("修改时间")
  @JsonFormat(pattern = StringPool.DATE_TIME_PATTERN)
  private LocalDateTime updateTime;

  public QueryProductBo() {

  }

  public QueryProductBo(Product dto) {

    super(dto);
  }

  @Override
  protected void afterInit(Product dto) {
    ProductCategoryService productCategoryService = ApplicationUtil.getBean(
        ProductCategoryService.class);
    ProductCategory productCategory = productCategoryService.findById(dto.getCategoryId());
    this.categoryName = productCategory.getName();

    if (StringUtil.isNotBlank(dto.getBrandId())) {
      ProductBrandService productBrandService = ApplicationUtil.getBean(ProductBrandService.class);
      ProductBrand brand = productBrandService.findById(dto.getBrandId());
      this.brandName = brand.getName();
    }

    ProductPurchaseService productPurchaseService = ApplicationUtil.getBean(ProductPurchaseService.class);
    ProductPurchase productPurchase = productPurchaseService.getById(dto.getId());
    if (productPurchase != null) {
      this.purchasePrice = productPurchase.getPrice();
    }

    ProductRetailService productRetailService = ApplicationUtil.getBean(ProductRetailService.class);
    ProductRetail productRetail = productRetailService.getById(dto.getId());
    if (productRetail != null) {
      this.retailPrice = productRetail.getPrice();
    }
  }
}
