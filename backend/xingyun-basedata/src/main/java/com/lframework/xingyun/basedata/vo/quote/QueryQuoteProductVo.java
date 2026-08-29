package com.lframework.xingyun.basedata.vo.quote;
import java.time.LocalDate; import javax.validation.constraints.NotNull; import lombok.Data;
/** 生效报价商品查询请求。 */ @Data public class QueryQuoteProductVo { @NotNull(message="订单日期不能为空！") private LocalDate orderDate; }
