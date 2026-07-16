package com.lframework.xingyun.sc.vo.purchase.receive;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ReceiveProductVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  private Integer seq;

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

  @ApiModelProperty("商品单位ID")
  private String unitId;

  /**
   * 采购价
   */
  @ApiModelProperty("采购价")
  private BigDecimal purchasePrice;

  /**
   * 收货数量
   */
  @ApiModelProperty("收货数量")
  private BigDecimal receiveNum;

  /**
   * 备注
   */
  @ApiModelProperty("备注")
  private String description;

  /**
   * 采购订单明细ID
   */
  @ApiModelProperty("采购订单明细ID")
  private String purchaseOrderDetailId;

  /**
   * 配送日期
   */
  @ApiModelProperty("配送日期")
  private LocalDate actualDate;

  /**
   * 生产日期
   */
  @ApiModelProperty("生产日期")
  private String productionDate;

  public BigDecimal getReceiveNum() {
    return receiveNum == null ? BigDecimal.ZERO : receiveNum;
  }

  public BigDecimal getPurchasePrice() {
    return purchasePrice == null ? BigDecimal.ZERO : purchasePrice;
  }
}
