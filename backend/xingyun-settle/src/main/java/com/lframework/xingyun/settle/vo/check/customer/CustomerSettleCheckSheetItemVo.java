package com.lframework.xingyun.settle.vo.check.customer;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 客户对账业务项。
 */
@Data
public class CustomerSettleCheckSheetItemVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /** 业务单据ID。 */
  @ApiModelProperty(value = "业务单据ID", required = true)
  @NotBlank(message = "业务单据不能为空！")
  private String bizId;

  /** 业务类型。 */
  @ApiModelProperty(value = "业务类型", required = true)
  @NotNull(message = "业务类型不能为空！")
  private Integer bizType;
}
