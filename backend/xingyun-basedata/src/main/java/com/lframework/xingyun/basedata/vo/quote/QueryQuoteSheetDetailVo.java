package com.lframework.xingyun.basedata.vo.quote;

import com.lframework.starter.web.core.vo.SortPageVo;
import com.lframework.xingyun.basedata.enums.quote.QuoteSheetStatus;
import java.time.LocalDate;
import lombok.Data;

/** 报价单商品明细查询请求。 */
@Data
public class QueryQuoteSheetDetailVo extends SortPageVo {
  /** 报价单名称。 */ private String quoteSheetName;
  /** 报价单状态。 */ private QuoteSheetStatus status;
  /** 是否询价商品。 */ private Boolean inquiryProduct;
  /** 生效开始日期。 */ private LocalDate startDate;
  /** 生效结束日期。 */ private LocalDate endDate;
  /** 商品名称或编号。 */ private String productKeyword;
}
