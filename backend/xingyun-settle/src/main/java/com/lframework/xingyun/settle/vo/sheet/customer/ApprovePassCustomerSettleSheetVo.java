package com.lframework.xingyun.settle.vo.sheet.customer;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 客户结算单审核通过请求。
 */
@Data
public class ApprovePassCustomerSettleSheetVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 结算单ID
   */
  @ApiModelProperty(value = "ID", required = true)
  @NotNull(message = "ID不能为空！")
  private String id;

  /**
   * 备注
   */
  @ApiModelProperty("备注")
  private String description;
}
