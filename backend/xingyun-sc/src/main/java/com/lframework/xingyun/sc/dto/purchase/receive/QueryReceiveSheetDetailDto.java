package com.lframework.xingyun.sc.dto.purchase.receive;

import com.lframework.starter.web.core.dto.BaseDto;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QueryReceiveSheetDetailDto implements BaseDto, Serializable {

  private static final long serialVersionUID = 1L;

  private String id;

  private String code;

  private String supplierId;

  private String supplierCode;

  private String supplierName;

  private String orderDate;

  private String receiveDate;

  private String purchaseOrderId;

  private String purchaseOrderCode;

  private String detailId;

  private String productId;

  private String productCode;

  private String productName;

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

  private BigDecimal taxPrice;

  private BigDecimal taxAmount;

  private Boolean isGift;

  private String description;

  /**
   * 单据备注
   */
  private String sheetDescription;

  /**
   * 生产日期
   */
  private String productionDate;

  private Integer settleStatus;

  private String createBy;

  private LocalDateTime createTime;

  private String approveBy;

  private LocalDateTime approveTime;

  private Integer status;
}
