package com.lframework.xingyun.sc.vo.sale.out;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BatchUpdateSaleOutSheetPriceVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  @ApiModelProperty(value = "销售出库明细ID列表", required = true)
  @NotEmpty(message = "请选择需要调整售价的明细数据！")
  private List<String> detailIds;

  @ApiModelProperty(value = "销售价", required = true)
  @NotNull(message = "销售价不能为空！")
  private BigDecimal taxPrice;
}
