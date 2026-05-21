package com.lframework.xingyun.sc.dto.sale.out;

import com.lframework.starter.web.core.dto.BaseDto;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class SaleOutSheetProductProfitDto implements BaseDto, Serializable {

  private static final long serialVersionUID = 1L;

  private String productId;

  private String productCode;

  private String productName;

  private String spec;

  private String unit;

  private BigDecimal saleNum;

  private BigDecimal salePrice;

  private BigDecimal purchasePrice;

  private BigDecimal salesAmount;

  private BigDecimal salesCost;

  private BigDecimal salesProfit;
}
