package com.lframework.xingyun.settle.vo.sheet.customer;

import com.lframework.starter.web.core.vo.PageVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 客户结算总览查询条件。
 */
@Data
public class QueryCustomerSettleOverviewVo extends PageVo {

  private static final long serialVersionUID = 1L;

  @ApiModelProperty("客户ID")
  private String customerId;
}
