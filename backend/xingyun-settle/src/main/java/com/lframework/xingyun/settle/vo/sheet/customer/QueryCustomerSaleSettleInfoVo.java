package com.lframework.xingyun.settle.vo.sheet.customer;

import com.lframework.starter.web.core.vo.PageVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 客户销售业务单据结算工作台查询条件。
 */
@Data
public class QueryCustomerSaleSettleInfoVo extends PageVo {

  private static final long serialVersionUID = 1L;

  @ApiModelProperty("单号")
  private String code;

  @ApiModelProperty("客户ID")
  private String customerId;

  @ApiModelProperty("业务类型：1-销售出库单，2-销售退货单")
  private Integer bizType;
}
