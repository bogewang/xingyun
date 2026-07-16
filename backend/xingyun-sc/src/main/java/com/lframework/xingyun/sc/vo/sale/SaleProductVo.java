package com.lframework.xingyun.sc.vo.sale;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SaleProductVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 商品ID
   */
  @ApiModelProperty("商品ID")
  private String productId;
  /**
   * 商品编号
   */
  @ApiModelProperty("商品编号")
  private String productCode;
  /**
   * 商品名称
   */
  @ApiModelProperty("商品名称")
  private String productName;

  /**
   * 商品规格
   */
  @ApiModelProperty("商品规格")
  private String spec;

  /**
   * 商品单位
   */
  @ApiModelProperty("商品单位")
  private String unit;

  /**
   * 原价
   */
  @ApiModelProperty("原价")
  private BigDecimal oriPrice;

  /**
   * 现价
   */
  @ApiModelProperty("现价")
  private BigDecimal taxPrice;

  /**
   * 折扣（%）
   */
  @ApiModelProperty("折扣（%）")
  private BigDecimal discountRate;

  /**
   * 销售数量
   */
  @ApiModelProperty("销售数量")
  private BigDecimal orderNum;

  /**
   * 备注
   */
  @ApiModelProperty("备注")
  private String description;
}
