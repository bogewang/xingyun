package com.lframework.xingyun.settle.vo.sheet;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class SettleSheetItemVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 单据ID
   */
  @ApiModelProperty("单据ID")
  private String id;

  /**
   * 未结算金额
   */
  @ApiModelProperty("未结算金额")
  private BigDecimal unSettleAmount;

  /**
   * 优惠金额
   */
  @ApiModelProperty("优惠金额")
  private BigDecimal discountAmount;

  /**
   * 备注
   */
  @ApiModelProperty("备注")
  private String description;

  /**
   * 结算金额
   */
  private BigDecimal settleAmount;
}
