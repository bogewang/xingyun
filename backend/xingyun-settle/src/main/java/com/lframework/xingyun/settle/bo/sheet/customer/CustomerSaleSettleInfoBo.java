package com.lframework.xingyun.settle.bo.sheet.customer;

import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 客户销售业务单据结算工作台信息。
 */
@Data
public class CustomerSaleSettleInfoBo implements Serializable {

  private static final long serialVersionUID = 1L;

  @ApiModelProperty("单据ID")
  private String id;

  @ApiModelProperty("业务类型")
  private Integer bizType;

  @ApiModelProperty("单号")
  private String code;

  @ApiModelProperty("客户ID")
  private String customerId;

  @ApiModelProperty("客户名称")
  private String customerName;

  @ApiModelProperty("单据金额")
  private BigDecimal totalAmount;

  @ApiModelProperty("已收金额")
  private BigDecimal receivedAmount;

  @ApiModelProperty("已结算金额")
  private BigDecimal settleAmount;

  @ApiModelProperty("未结算金额")
  private BigDecimal unSettleAmount;

  @ApiModelProperty("结算状态")
  private Integer settleStatus;
}
