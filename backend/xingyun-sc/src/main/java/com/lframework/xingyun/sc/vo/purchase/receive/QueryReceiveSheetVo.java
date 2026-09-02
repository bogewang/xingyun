package com.lframework.xingyun.sc.vo.purchase.receive;

import com.lframework.starter.web.core.components.validation.IsEnum;
import com.lframework.starter.web.core.vo.SortPageVo;
import com.lframework.xingyun.sc.enums.ReceiveSheetStatus;
import com.lframework.xingyun.sc.enums.SettleStatus;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QueryReceiveSheetVo extends SortPageVo {

  private static final long serialVersionUID = 1L;

  /**
   * 单号
   */
  @ApiModelProperty("单号")
  private String code;

  /**
   * 单据备注
   */
  @ApiModelProperty("单据备注")
  private String sheetDescription;

  /**
   * 商品名称
   */
  @ApiModelProperty("商品名称")
  private String productName;

  private List<String> idList;

  /**
   * 供应商ID
   */
  @ApiModelProperty("供应商ID")
  private String supplierId;

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
  @IsEnum(message = "状态格式不正确！", enumClass = ReceiveSheetStatus.class)
  private Integer status;

  /**
   * 采购员ID
   */
  @ApiModelProperty("采购员ID")
  private String purchaserId;

  /**
   * 采购订单号
   */
  @ApiModelProperty("采购订单号")
  private String purchaseOrderCode;

  /**
   * 结算状态
   */
  @ApiModelProperty("结算状态")
  @IsEnum(message = "结算状态格式不正确！", enumClass = SettleStatus.class)
  private Integer settleStatus;

  /**
   * 是否已付完
   */
  @ApiModelProperty("是否已付完")
  private Boolean fullyPaid;

  /**
   * 是否询价商品
   */
  @ApiModelProperty("是否询价商品")
  private Boolean inquiryProduct;

  /**
   * 已付金额起始值
   */
  @ApiModelProperty("已付金额起始值")
  private BigDecimal paidAmountStart;

  /**
   * 已付金额截止值
   */
  @ApiModelProperty("已付金额截止值")
  private BigDecimal paidAmountEnd;

  /**
   * 未付金额起始值
   */
  @ApiModelProperty("未付金额起始值")
  private BigDecimal unpaidAmountStart;

  /**
   * 未付金额截止值
   */
  @ApiModelProperty("未付金额截止值")
  private BigDecimal unpaidAmountEnd;
}
