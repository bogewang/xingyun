package com.lframework.xingyun.sc.dto.sale.out;

import com.lframework.starter.web.core.dto.BaseDto;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QuerySaleOutSheetDetailDto implements BaseDto, Serializable {

  private static final long serialVersionUID = 1L;

  private String id;

  private String code;

  private String customerId;

  private String customerCode;

  private String customerName;

  private String orderDate;

  private String saleOrderId;

  private String saleOrderCode;

  private String detailId;

  private String productId;

  private String productCode;

  private String productName;

  private String skuCode;

  private String externalCode;

  private String spec;

  private String unit;

  private String categoryName;

  private String brandName;

  private BigDecimal orderNum;

  private BigDecimal taxPrice;

  private BigDecimal taxAmount;

  private BigDecimal costPrice;

  private BigDecimal totalProfit;

  private Boolean isGift;

  private String description;

  private Integer settleStatus;

  private String createBy;

  private LocalDateTime createTime;

  private String approveBy;

  private LocalDateTime approveTime;

  private Integer status;
}
