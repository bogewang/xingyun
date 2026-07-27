package com.lframework.xingyun.settle.vo.sheet.customer;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 客户结算单修改请求。
 */
@Data
public class UpdateCustomerSettleSheetVo extends CreateCustomerSettleSheetVo {

  private static final long serialVersionUID = 1L;

  /**
   * 结算单ID
   */
  @ApiModelProperty(value = "ID", required = true)
  @NotNull(message = "ID不能为空！")
  private String id;
}
