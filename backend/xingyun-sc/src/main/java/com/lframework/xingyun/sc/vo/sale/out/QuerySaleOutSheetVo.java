package com.lframework.xingyun.sc.vo.sale.out;

import com.lframework.starter.web.core.components.validation.IsEnum;
import com.lframework.starter.web.core.vo.SortPageVo;
import com.lframework.xingyun.sc.enums.SaleOutSheetStatus;
import com.lframework.xingyun.sc.enums.SettleStatus;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuerySaleOutSheetVo extends SortPageVo {

  private static final long serialVersionUID = 1L;

  /**
   * 单号
   */
  @ApiModelProperty("单号")
  private String code;

  /**
   * 商品名称
   */
  @ApiModelProperty("商品名称")
  private String productName;

  /**
   * 商品ID
   */
  @ApiModelProperty("商品ID")
  private String productId;

  /**
   * 规格型号
   */
  @ApiModelProperty("规格型号")
  private String productSpec;

  /**
   * 单据ID列表
   */
  @ApiModelProperty("单据ID列表")
  private List<String> idList;

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

  /**
   * 是否仅查询未被结算交易占用的单据
   */
  @ApiModelProperty("是否仅查询未被结算交易占用的单据")
  private Boolean requireTxIdNull;

  /**
   * 是否已付完
   */
  @ApiModelProperty("是否已付完")
  private Boolean fullyPaid;

  /**
   * 是否已录采购
   */
  @ApiModelProperty("是否已录采购")
  private Boolean hasCostPrice;

  /**
   * 是否仅查询负毛利商品
   */
  @ApiModelProperty("是否仅查询负毛利商品")
  private Boolean onlyNegativeProfit;

  /**
   * 是否录完所有成本
   */
  @ApiModelProperty("是否录完所有成本")
  private Boolean fillAllCost;

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

  /**
   * 是否仅查询多价格商品
   */
  @ApiModelProperty("是否仅查询多价格商品")
  private Boolean onlyMultiPrice;

  /**
   * 商品分类ID列表
   */
  @ApiModelProperty("商品分类ID列表")
  private List<String> categoryIdList;
}
