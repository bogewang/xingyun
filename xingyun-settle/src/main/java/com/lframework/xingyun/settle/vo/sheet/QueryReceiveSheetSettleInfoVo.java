package com.lframework.xingyun.settle.vo.sheet;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class QueryReceiveSheetSettleInfoVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  @ApiModelProperty("收货单ID列表")
  @NotEmpty(message = "收货单ID不能为空！")
  private List<String> ids;
}
