package com.lframework.xingyun.settle.vo.sheet.customer;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Data;

@Data
public class CustomerSettleSheetItemVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 单据ID
   */
  @ApiModelProperty("业务单据ID")
  private String bizId;

  /**
   * 业务类型，1-销售出库单，2-销售退货单。
   */
  @ApiModelProperty("业务类型")
  private Integer bizType;
}
