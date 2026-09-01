package com.lframework.xingyun.basedata.vo.quote;
import java.time.LocalDate; import javax.validation.constraints.NotNull; import lombok.Data;
/** 按订单日期查询全部生效报价商品的请求，不按商品 ID 筛选。 */ @Data public class QueryQuoteProductVo { @NotNull(message="订单日期不能为空！") private LocalDate orderDate; }
