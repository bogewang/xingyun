package com.lframework.xingyun.sc.vo.sale.out;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 同步询价商品销售价参数。
 */
@Data
public class SyncInquirySalePriceVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  @ApiModelProperty(value = "订单开始日期", required = true)
  @NotNull(message = "订单开始日期不能为空！")
  private LocalDate startDate;

  @ApiModelProperty(value = "订单结束日期", required = true)
  @NotNull(message = "订单结束日期不能为空！")
  private LocalDate endDate;
}
