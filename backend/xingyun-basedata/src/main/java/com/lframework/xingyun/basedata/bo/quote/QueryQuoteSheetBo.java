package com.lframework.xingyun.basedata.bo.quote;
import com.lframework.xingyun.basedata.enums.quote.QuoteSheetStatus; import java.time.*; import lombok.Data;
/** 报价单列表响应。 */ @Data public class QueryQuoteSheetBo { private String id; private String name; private LocalDate startDate; private LocalDate endDate; private QuoteSheetStatus status; private String description; private String createBy; private LocalDateTime createTime; }
