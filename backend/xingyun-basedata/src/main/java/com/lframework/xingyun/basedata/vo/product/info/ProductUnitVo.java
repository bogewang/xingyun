package com.lframework.xingyun.basedata.vo.product.info;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductUnitVo implements BaseVo, Serializable {
  @ApiModelProperty("单位名称") @NotBlank(message = "单位名称不能为空！")
  private String unitName;
  @ApiModelProperty("换算率：1 个该单位等于多少主单位") @NotNull(message = "换算率不能为空！")
  private BigDecimal conversionRate;
  @ApiModelProperty("是否启用")
  private Boolean available = Boolean.TRUE;
  @ApiModelProperty("排序")
  private Integer sortNo;
}
