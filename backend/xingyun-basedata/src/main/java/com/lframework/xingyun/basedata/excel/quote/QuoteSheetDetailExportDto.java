package com.lframework.xingyun.basedata.excel.quote;

import java.math.BigDecimal;
import lombok.Data;

/** 报价单商品明细导出数据。 */
@Data
public class QuoteSheetDetailExportDto {
  private String quoteSheetName;
  private String startDate;
  private String endDate;
  private String status;
  private String description;
  private String productCode;
  private String productName;
  private String shortName;
  private String spec;
  private String unit;
  private BigDecimal salePrice;
}
