package com.lframework.xingyun.sc.bo.purchase.receive;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.xingyun.sc.dto.purchase.receive.QueryReceiveSheetDetailDto;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QueryReceiveSheetDetailBo extends BaseBo<QueryReceiveSheetDetailDto> {

  @ApiModelProperty("单据ID")
  private String id;

  @ApiModelProperty("单据号")
  private String code;

  @ApiModelProperty("供应商ID")
  private String supplierId;

  @ApiModelProperty("供应商编号")
  private String supplierCode;

  @ApiModelProperty("供应商名称")
  private String supplierName;

  @ApiModelProperty("订单日期")
  private String orderDate;

  @ApiModelProperty("到货日期")
  private String receiveDate;

  @ApiModelProperty("采购订单ID")
  private String purchaseOrderId;

  @ApiModelProperty("采购订单号")
  private String purchaseOrderCode;

  @ApiModelProperty("明细ID")
  private String detailId;

  @ApiModelProperty("商品ID")
  private String productId;

  @ApiModelProperty("商品编号")
  private String productCode;

  @ApiModelProperty("商品名称")
  private String productName;

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

  @ApiModelProperty("收货数量")
  private BigDecimal orderNum;

  @ApiModelProperty("采购价")
  private BigDecimal taxPrice;

  @ApiModelProperty("采购金额")
  private BigDecimal taxAmount;

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

  public QueryReceiveSheetDetailBo(QueryReceiveSheetDetailDto dto) {
    super(dto);
  }
}
