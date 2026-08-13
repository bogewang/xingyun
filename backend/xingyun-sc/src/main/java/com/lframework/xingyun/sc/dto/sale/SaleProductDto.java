package com.lframework.xingyun.sc.dto.sale;

import com.lframework.starter.web.core.dto.BaseDto;
import com.lframework.xingyun.basedata.entity.ProductUnit;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SaleProductDto implements BaseDto, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * ID
   */
  private String id;

  /**
   * 编号
   */
  private String code;

  /**
   * 名称
   */
  private String name;

  /**
   * 简称
   */
  private String shortName;

  /**
   * 分类ID
   */
  private String categoryId;

  /**
   * 分类名称
   */
  private String categoryName;

  /**
   * 品牌ID
   */
  private String brandId;

  /**
   * 品牌名称
   */
  private String brandName;

  /**
   * SKU
   */
  private String skuCode;

  /**
   * 简码
   */
  private String externalCode;

  /**
   * 规格
   */
  private String spec;

  /**
   * 单位
   */
  private String unit;

  /** 可选交易单位（含主单位）。 */
  private List<ProductUnit> units;

  /**
   * 采购价
   */
  private BigDecimal purchasePrice;

  /**
   * 销售价
   */
  private BigDecimal salePrice;

  /**
   * 税率（%）
   */
  private BigDecimal taxRate;

  /**
   * 是否询价商品
   */
  private Boolean inquiryProduct;

  /**
   * 备注
   */
  private String remark;

  /**
   * 状态
   */
  private Boolean available;
}
