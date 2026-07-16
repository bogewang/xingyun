package com.lframework.xingyun.sc.bo.sale.out;

import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class SaleOutSheetProfitSummaryBo {

  @ApiModelProperty("销售笔数")
  private Long saleCount;

  @ApiModelProperty("销售额")
  private BigDecimal salesAmount;

  @ApiModelProperty("成本")
  private BigDecimal salesCost;

  @ApiModelProperty("销售毛利")
  private BigDecimal salesProfit;

  @ApiModelProperty("其他收入")
  private BigDecimal otherIncome;

  @ApiModelProperty("其他支出")
  private BigDecimal otherExpense;

  @ApiModelProperty("净利润")
  private BigDecimal netProfit;

  @ApiModelProperty("其他费用")
  private BigDecimal otherFee;
}
