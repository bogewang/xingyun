package com.lframework.xingyun.sc.vo.purchase.receive;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量更新采购收货单备注参数
 */
@Data
public class BatchUpdateReceiveSheetDescriptionVo {

  /**
   * 收货单ID列表
   */
  @ApiModelProperty(value = "收货单ID列表", required = true)
  @NotEmpty(message = "请选择需要更新备注的采购收货单！")
  private List<String> ids;

  /**
   * 备注
   */
  @ApiModelProperty("备注")
  private String description;
}
