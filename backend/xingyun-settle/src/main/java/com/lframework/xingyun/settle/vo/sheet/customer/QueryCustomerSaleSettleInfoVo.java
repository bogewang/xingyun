package com.lframework.xingyun.settle.vo.sheet.customer;

import com.lframework.starter.web.core.vo.PageVo;
import com.lframework.starter.web.core.components.validation.IsEnum;
import com.lframework.xingyun.settle.enums.CustomerSaleSettleBizType;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
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
  @NotNull(message = "业务类型不能为空！")
  @IsEnum(message = "业务类型不正确！", enumClass = CustomerSaleSettleBizType.class)
  private Integer bizType;
}
