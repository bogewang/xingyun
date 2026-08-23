package com.lframework.xingyun.basedata.vo.supplier;

import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批量更新供应商启用状态请求。
 */
@Data
public class UpdateSupplierAvailableVo implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 供应商 ID 列表。
   */
  @ApiModelProperty("供应商 ID 列表")
  @NotEmpty(message = "供应商 ID 不能为空！")
  private List<String> ids;

  /**
   * 目标启用状态。
   */
  @ApiModelProperty("是否启用")
  @NotNull(message = "启用状态不能为空！")
  private Boolean available;
}
