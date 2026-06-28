package com.lframework.xingyun.sc.dto.stock;

import com.lframework.starter.web.core.dto.BaseDto;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ProductStockPendingCostResolveDto implements BaseDto, Serializable {

  private static final long serialVersionUID = 1L;

  private BigDecimal settledNum;

  private BigDecimal settledTaxAmount;

  private BigDecimal remainNum;

  private BigDecimal remainTaxAmount;
}
