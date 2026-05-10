package com.lframework.xingyun.sc.vo.sale.out;

import com.lframework.starter.web.core.components.validation.IsEnum;
import com.lframework.starter.web.core.vo.SortPageVo;
import com.lframework.xingyun.sc.enums.SaleOutSheetStatus;
import com.lframework.xingyun.sc.enums.SettleStatus;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QuerySaleOutSheetVo extends SortPageVo {

  private static final long serialVersionUID = 1L;

  /**
   * 单号
   */
  @ApiModelProperty("单号")
  private String code;

  /**
   * 客户ID
   */
  @ApiModelProperty("客户ID")
  private String customerId;

  /**
   * 仓库ID
   */
  @ApiModelProperty("仓库ID")
  private String scId;

  /**
   * 操作人ID
   */
  @ApiModelProperty("操作人ID")
  private String createBy;

  /**
   * 订单起始日期
   */
  @ApiModelProperty("订单起始日期")
  private LocalDate orderDateStart;

  /**
   * 订单截止日期
   */
  @ApiModelProperty("订单截止日期")
  private LocalDate orderDateEnd;

  /**
   * 审核人ID
   */
  @ApiModelProperty("审核人ID")
  private String approveBy;

  /**
   * 审核起始时间
   */
  @ApiModelProperty("审核起始时间")
  private LocalDateTime approveStartTime;

  /**
   * 审核截止时间
   */
  @ApiModelProperty("审核截止时间")
  private LocalDateTime approveEndTime;

  /**
   * 状态
   */
  @ApiModelProperty("状态")
  @IsEnum(message = "状态格式不正确！", enumClass = SaleOutSheetStatus.class)
  private Integer status;

  /**
   * 销售员ID
   */
  @ApiModelProperty("销售员ID")
  private String salerId;

  /**
   * 销售订单号
   */
  @ApiModelProperty("销售订单号")
  private String saleOrderCode;

  /**
   * 结算状态
   */
  @ApiModelProperty("结算状态")
  @IsEnum(message = "结算状态格式不正确！", enumClass = SettleStatus.class)
  private Integer settleStatus;
}
