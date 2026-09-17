package com.lframework.xingyun.sc.vo.sale.out;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.time.LocalDate;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 按订单日期合并销售出库商品参数。
 */
@Data
public class MergeSaleOutSheetProductVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /** 订单开始日期。 */
  @ApiModelProperty(value = "订单开始日期", required = true)
  @NotNull(message = "订单开始日期不能为空！")
  private LocalDate startDate;

  /** 订单结束日期。 */
  @ApiModelProperty(value = "订单结束日期", required = true)
  @NotNull(message = "订单结束日期不能为空！")
  private LocalDate endDate;
}
