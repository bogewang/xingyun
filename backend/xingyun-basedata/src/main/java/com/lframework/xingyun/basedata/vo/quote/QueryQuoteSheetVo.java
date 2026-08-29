package com.lframework.xingyun.basedata.vo.quote;
import com.lframework.starter.web.core.vo.SortPageVo; import com.lframework.xingyun.basedata.enums.quote.QuoteSheetStatus; import java.time.LocalDate; import lombok.Data;
/** 报价单查询请求。 */ @Data public class QueryQuoteSheetVo extends SortPageVo { private String name; private QuoteSheetStatus status; private LocalDate startDate; private LocalDate endDate; }
