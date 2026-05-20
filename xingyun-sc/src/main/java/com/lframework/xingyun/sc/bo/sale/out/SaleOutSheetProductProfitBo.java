package com.lframework.xingyun.sc.bo.sale.out;

import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetProductProfitDto;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class SaleOutSheetProductProfitBo extends BaseBo<SaleOutSheetProductProfitDto> {

  @ApiModelProperty("商品ID")
  private String productId;

  @ApiModelProperty("商品编号")
  private String productCode;

  @ApiModelProperty("商品名称")
  private String productName;

  @ApiModelProperty("规格型号")
  private String spec;

  @ApiModelProperty("单位")
  private String unit;

  @ApiModelProperty("销售数量")
  private BigDecimal saleNum;

  @ApiModelProperty("销售均价")
  private BigDecimal salePrice;

  @ApiModelProperty("销售金额")
  private BigDecimal salesAmount;

  @ApiModelProperty("销售成本")
  private BigDecimal salesCost;

  @ApiModelProperty("销售毛利")
  private BigDecimal salesProfit;

  public SaleOutSheetProductProfitBo(SaleOutSheetProductProfitDto dto) {
    super(dto);
  }
}
