package com.lframework.xingyun.sc.vo.sale.out;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 合并销售出库商品参数。
 */
@Data
public class MergeSaleOutSheetProductVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /** 销售出库单ID列表。 */
  @ApiModelProperty(value = "销售出库单ID列表", required = true)
  @NotEmpty(message = "请选择要合并商品的销售出库单！")
  private List<String> ids;
}
