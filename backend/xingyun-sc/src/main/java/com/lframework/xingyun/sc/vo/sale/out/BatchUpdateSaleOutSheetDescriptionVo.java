package com.lframework.xingyun.sc.vo.sale.out;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量更新销售出库单备注参数
 */
@Data
public class BatchUpdateSaleOutSheetDescriptionVo {

  /**
   * 销售出库单ID列表
   */
  @ApiModelProperty(value = "销售出库单ID列表", required = true)
  @NotEmpty(message = "请选择需要更新备注的销售出库单！")
  private List<String> ids;

  /**
   * 备注
   */
  @ApiModelProperty("备注")
  private String description;
}
