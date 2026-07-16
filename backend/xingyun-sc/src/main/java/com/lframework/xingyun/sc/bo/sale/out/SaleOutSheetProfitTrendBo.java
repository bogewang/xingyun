package com.lframework.xingyun.sc.bo.sale.out;

import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetProfitTrendDto;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class SaleOutSheetProfitTrendBo extends BaseBo<SaleOutSheetProfitTrendDto> {

  @ApiModelProperty("销售日期")
  private LocalDate orderDate;

  @ApiModelProperty("销售金额")
  private BigDecimal salesAmount;

  @ApiModelProperty("利润")
  private BigDecimal salesProfit;

  @ApiModelProperty("毛利率")
  private BigDecimal profitRate;

  public SaleOutSheetProfitTrendBo(SaleOutSheetProfitTrendDto dto) {
    super(dto);
  }
}
