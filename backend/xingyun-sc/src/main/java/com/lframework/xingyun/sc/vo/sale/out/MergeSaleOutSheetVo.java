package com.lframework.xingyun.sc.vo.sale.out;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@Data
public class MergeSaleOutSheetVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 销售出库单ID列表
   */
  @ApiModelProperty(value = "销售出库单ID列表", required = true)
  @NotEmpty(message = "请选择要合并的销售出库单！")
  private List<String> ids;
}
