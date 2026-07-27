package com.lframework.xingyun.settle.vo.sheet.customer;

import com.lframework.starter.web.core.vo.PageVo;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 客户结算总览查询条件。
 */
@Data
public class QueryCustomerSettleOverviewVo extends PageVo {

  private static final long serialVersionUID = 1L;

  @ApiModelProperty("客户ID")
  private String customerId;

  /**
   * 单据起始时间。
   */
  @ApiModelProperty("单据起始时间")
  private LocalDateTime orderStartTime;

  /**
   * 单据截止时间。
   */
  @ApiModelProperty("单据截止时间")
  private LocalDateTime orderEndTime;
}
