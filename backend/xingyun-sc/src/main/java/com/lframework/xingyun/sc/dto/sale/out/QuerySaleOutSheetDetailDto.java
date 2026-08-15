package com.lframework.xingyun.sc.dto.sale.out;

import com.lframework.starter.web.core.dto.BaseDto;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QuerySaleOutSheetDetailDto implements BaseDto, Serializable {

  private static final long serialVersionUID = 1L;

  private String id;

  private String code;

  private String customerId;

  private String customerCode;

  private String customerName;

  private String orderDate;

  /**
   * 计划日期
   */
  private String planDate;

  private String saleOrderId;

  private String saleOrderCode;

  private String detailId;

  private String productId;

  private String productCode;

  private String productName;

  /**
   * 商品备注
   */
  private String productRemark;

  /**
   * 商品备注二
   */
  private String productRemark2;

  /**
   * 是否询价商品
   */
  private Boolean inquiryProduct;

  private String skuCode;

  private String externalCode;

  private String spec;

  private String unit;

  private String categoryName;

  private String brandName;

  private BigDecimal orderNum;

  /**
   * 验收数量，使用交易单位
   */
  private BigDecimal confirmNum;

  private BigDecimal taxPrice;

  private BigDecimal taxAmount;

  /**
   * 验收金额，根据验收数量和销售单价计算
   */
  private BigDecimal confirmAmt;

  private BigDecimal costPrice;

  private BigDecimal totalProfit;

  private Boolean isGift;

  private String description;

  /**
   * 单据备注
   */
  private String sheetDescription;

  private Integer settleStatus;

  private String createBy;

  private LocalDateTime createTime;

  private String approveBy;

  private LocalDateTime approveTime;

  private Integer status;

  private String supplierId;
}
