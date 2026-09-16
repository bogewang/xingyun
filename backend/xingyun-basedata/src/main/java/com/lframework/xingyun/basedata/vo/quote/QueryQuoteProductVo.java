package com.lframework.xingyun.basedata.vo.quote;
import java.time.LocalDate;
import javax.validation.constraints.NotNull;
import lombok.Data;

/** 按项目和订单日期查询生效报价商品的请求。 */
@Data
public class QueryQuoteProductVo {
  /** 项目ID。 */
  private String projectId;

  /** 订单日期。 */
  @NotNull(message = "订单日期不能为空！")
  private LocalDate orderDate;
}
