package com.lframework.xingyun.settle.bo.sheet.customer;

import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 客户结算总览信息。
 */
@Data
public class CustomerSettleOverviewBo implements Serializable {

  private static final long serialVersionUID = 1L;

  @ApiModelProperty("客户ID")
  private String customerId;

  @ApiModelProperty("客户编号")
  private String customerCode;

  @ApiModelProperty("客户名称")
  private String customerName;

  @ApiModelProperty("待对账单据数")
  private Integer unCheckCount;

  @ApiModelProperty("待对账金额")
  private BigDecimal unCheckAmount;

  @ApiModelProperty("待结算单据数")
  private Integer unSettleCount;

  @ApiModelProperty("待结算金额")
  private BigDecimal unSettleAmount;

  @ApiModelProperty("部分结算单据数")
  private Integer partSettleCount;

  @ApiModelProperty("部分结算金额")
  private BigDecimal partSettleAmount;

  @ApiModelProperty("已结算单据数")
  private Integer settledCount;

  @ApiModelProperty("已结算金额")
  private BigDecimal settledAmount;
}
