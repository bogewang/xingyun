package com.lframework.xingyun.settle.vo.check.customer;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 客户对账单直接确认请求。
 */
@Data
public class CreateCustomerSettleCheckSheetVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /** 客户ID。 */
  @ApiModelProperty(value = "客户ID", required = true)
  @NotBlank(message = "客户不能为空！")
  private String customerId;

  /** 确认对账金额。 */
  @ApiModelProperty(value = "确认对账金额", required = true)
  @NotNull(message = "确认对账金额不能为空！")
  private BigDecimal checkAmount;

  /** 备注。 */
  @ApiModelProperty("备注")
  private String description;

  /** 对账业务项。 */
  @ApiModelProperty(value = "对账业务项", required = true)
  @NotEmpty(message = "对账业务项不能为空！")
  private List<CustomerSettleCheckSheetItemVo> items;
}
