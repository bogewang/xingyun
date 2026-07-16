package com.lframework.xingyun.sc.vo.sale.out;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateSaleOutSheetDescriptionVo {

  /**
   * 销售出库单ID
   */
  @ApiModelProperty(value = "销售出库单ID", required = true)
  @NotBlank(message = "销售出库单ID不能为空！")
  private String id;

  /**
   * 备注
   */
  @ApiModelProperty("备注")
  private String description;
}
