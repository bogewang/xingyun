package com.lframework.xingyun.sc.vo.purchase.receive;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ConfirmReceiveSheetCheckVo {

  @ApiModelProperty("单据ID")
  @NotEmpty(message = "请选择需要对账的采购入库单！")
  private List<String> ids;

  @ApiModelProperty("备注")
  private String description;
}
