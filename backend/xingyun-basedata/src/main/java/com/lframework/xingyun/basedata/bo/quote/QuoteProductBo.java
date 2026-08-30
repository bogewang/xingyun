package com.lframework.xingyun.basedata.bo.quote;
import java.math.BigDecimal; import lombok.Data;
/** 生效报价商品响应。 */ @Data public class QuoteProductBo { private String quoteSheetId; private String sourceId; private String productId; private String code; private String name; private String shortName; private String skuCode; private String spec; private String unit; private BigDecimal salePrice; }
