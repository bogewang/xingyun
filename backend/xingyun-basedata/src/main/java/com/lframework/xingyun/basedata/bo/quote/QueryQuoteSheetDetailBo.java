package com.lframework.xingyun.basedata.bo.quote;

import com.lframework.xingyun.basedata.enums.quote.QuoteSheetStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 报价单商品明细查询响应。 */
@Data
public class QueryQuoteSheetDetailBo {
  /** 报价单ID。 */ private String quoteSheetId;
  /** 报价单名称。 */ private String quoteSheetName;
  /** 生效开始日期。 */ private LocalDate startDate;
  /** 生效结束日期。 */ private LocalDate endDate;
  /** 报价单状态。 */ private QuoteSheetStatus status;
  /** 明细ID。 */ private String detailId;
  /** 商品ID。 */ private String productId;
  /** 商品编号。 */ private String productCode;
  /** 商品名称。 */ private String productName;
  /** 商品规格。 */ private String spec;
  /** 商品单位。 */ private String unit;
  /** 销售单价。 */ private BigDecimal salePrice;
  /** 是否询价商品。 */ private Boolean inquiryProduct;
}
