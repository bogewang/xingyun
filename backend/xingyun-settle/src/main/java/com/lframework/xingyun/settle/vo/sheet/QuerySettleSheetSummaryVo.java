package com.lframework.xingyun.settle.vo.sheet;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 供应商结算汇总查询条件
 */
@Data
public class QuerySettleSheetSummaryVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  @ApiModelProperty("供应商ID")
  private String supplierId;

  @ApiModelProperty("单据起始时间")
  private LocalDateTime orderStartTime;

  @ApiModelProperty("单据截止时间")
  private LocalDateTime orderEndTime;
}
