package com.lframework.xingyun.sc.bo.sale.out;

import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetProductProfitTrendDto;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class SaleOutSheetProductProfitTrendBo extends BaseBo<SaleOutSheetProductProfitTrendDto> {

  @ApiModelProperty("单据日期")
  private LocalDate orderDate;

  @ApiModelProperty("销售金额")
  private BigDecimal salesAmount;

  @ApiModelProperty("销售毛利")
  private BigDecimal salesProfit;

  @ApiModelProperty("毛利率")
  private BigDecimal profitRate;

  public SaleOutSheetProductProfitTrendBo(SaleOutSheetProductProfitTrendDto dto) {
    super(dto);
  }
}
