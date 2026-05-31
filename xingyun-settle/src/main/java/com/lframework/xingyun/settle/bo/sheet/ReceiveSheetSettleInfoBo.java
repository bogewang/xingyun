package com.lframework.xingyun.settle.bo.sheet;

import com.lframework.xingyun.sc.bo.purchase.receive.QueryReceiveSheetBo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ReceiveSheetSettleInfoBo extends QueryReceiveSheetBo implements Serializable {

  private static final long serialVersionUID = 1L;

  @ApiModelProperty("收货单ID")
  private String bizSheetId;

  @ApiModelProperty("对账金额")
  private BigDecimal checkAmount;

  @ApiModelProperty("对账备注")
  private String checkDescription;

  @ApiModelProperty("结算金额")
  private BigDecimal settleAmount;

  @ApiModelProperty("结算备注")
  private String settleDescription;
}
