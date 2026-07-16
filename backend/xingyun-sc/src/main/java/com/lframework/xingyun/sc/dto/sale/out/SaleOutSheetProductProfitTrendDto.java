package com.lframework.xingyun.sc.dto.sale.out;

import com.lframework.starter.web.core.dto.BaseDto;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class SaleOutSheetProductProfitTrendDto implements BaseDto, Serializable {

  private static final long serialVersionUID = 1L;

  private LocalDate orderDate;

  private BigDecimal salesAmount;

  private BigDecimal salesProfit;

  private BigDecimal profitRate;
}
