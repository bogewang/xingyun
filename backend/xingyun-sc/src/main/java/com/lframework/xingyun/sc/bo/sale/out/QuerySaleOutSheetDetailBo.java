package com.lframework.xingyun.sc.bo.sale.out;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.xingyun.sc.dto.sale.out.QuerySaleOutSheetDetailDto;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QuerySaleOutSheetDetailBo extends BaseBo<QuerySaleOutSheetDetailDto> {

  @ApiModelProperty("单据ID")
  private String id;

  @ApiModelProperty("单据号")
  private String code;

  @ApiModelProperty("客户ID")
  private String customerId;

  @ApiModelProperty("客户编号")
  private String customerCode;

  @ApiModelProperty("客户名称")
  private String customerName;

  @ApiModelProperty("订单日期")
  private String orderDate;

  @ApiModelProperty("计划日期")
  private String planDate;

  @ApiModelProperty("销售订单ID")
  private String saleOrderId;

  @ApiModelProperty("销售订单号")
  private String saleOrderCode;

  @ApiModelProperty("明细ID")
  private String detailId;

  @ApiModelProperty("商品ID")
  private String productId;

  @ApiModelProperty("商品编号")
  private String productCode;

  @ApiModelProperty("商品名称")
  private String productName;

  /**
   * 是否询价商品
   */
  @ApiModelProperty("是否询价商品")
  private Boolean inquiryProduct;

  @ApiModelProperty("商品SKU编号")
  private String skuCode;

  @ApiModelProperty("商品简码")
  private String externalCode;

  @ApiModelProperty("规格")
  private String spec;

  @ApiModelProperty("单位")
  private String unit;

  @ApiModelProperty("商品分类")
  private String categoryName;

  @ApiModelProperty("商品品牌")
  private String brandName;

  @ApiModelProperty("出库数量")
  private BigDecimal orderNum;

  @ApiModelProperty("验收数量")
  private BigDecimal confirmNum;

  @ApiModelProperty("销售价")
  private BigDecimal taxPrice;

  @ApiModelProperty("销售金额")
  private BigDecimal taxAmount;

  @ApiModelProperty("验收金额")
  private BigDecimal confirmAmt;

  @ApiModelProperty("成本价")
  private BigDecimal costPrice;

  @ApiModelProperty("利润")
  private BigDecimal totalProfit;

  @ApiModelProperty("是否赠品")
  private Boolean isGift;

  @ApiModelProperty("备注")
  private String description;

  @ApiModelProperty("结算状态")
  private Integer settleStatus;

  @ApiModelProperty("操作人")
  private String createBy;

  @ApiModelProperty("操作时间")
  @JsonFormat(pattern = StringPool.DATE_TIME_PATTERN)
  private LocalDateTime createTime;

  @ApiModelProperty("审核人")
  private String approveBy;

  @ApiModelProperty("审核时间")
  @JsonFormat(pattern = StringPool.DATE_TIME_PATTERN)
  private LocalDateTime approveTime;

  @ApiModelProperty("状态")
  private Integer status;

  public QuerySaleOutSheetDetailBo(QuerySaleOutSheetDetailDto dto) {
    super(dto);
  }
}
