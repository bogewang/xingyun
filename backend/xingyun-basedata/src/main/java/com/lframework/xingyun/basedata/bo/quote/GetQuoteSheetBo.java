package com.lframework.xingyun.basedata.bo.quote;
import com.lframework.xingyun.basedata.enums.quote.QuoteSheetStatus; import java.time.LocalDate; import java.util.List; import lombok.Data;
/** 报价单详情响应。 */ @Data public class GetQuoteSheetBo { private String id; private String code; private String name; private LocalDate startDate; private LocalDate endDate; private QuoteSheetStatus status; private String description; private List<QuoteProductBo> products; }
