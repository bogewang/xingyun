package com.lframework.xingyun.sc.excel.sale.out;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetProductProfitDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Data;

@Data
public class SaleOutSheetProductProfitExportModel extends BaseBo<SaleOutSheetProductProfitDto>
    implements ExcelModel {

  @ExcelProperty("商品名称")
  private String productName;

  @ExcelProperty("规格型号")
  private String spec;

  @ExcelProperty("单位")
  private String unit;

  @ExcelProperty("销售数量")
  private BigDecimal saleNum;

  @ExcelProperty("销售均价")
  private BigDecimal salePrice;

  @ExcelProperty("采购均价")
  private BigDecimal purchasePrice;

  @ExcelProperty("销售金额")
  private BigDecimal salesAmount;

  @ExcelProperty("销售成本")
  private BigDecimal salesCost;

  @ExcelProperty("销售毛利")
  private BigDecimal salesProfit;

  @ExcelProperty("销售毛利率")
  private String profitRate;

  public SaleOutSheetProductProfitExportModel() {
  }

  public SaleOutSheetProductProfitExportModel(SaleOutSheetProductProfitDto dto) {
    super(dto);
  }

  @Override
  public <A> BaseBo<SaleOutSheetProductProfitDto> convert(SaleOutSheetProductProfitDto dto) {
    return this;
  }

  @Override
  protected void afterInit(SaleOutSheetProductProfitDto dto) {
    this.productName = dto.getProductName();
    this.spec = dto.getSpec();
    this.unit = dto.getUnit();
    this.saleNum = defaultValue(dto.getSaleNum());
    this.salePrice = defaultValue(dto.getSalePrice());
    this.purchasePrice = defaultValue(dto.getPurchasePrice());
    this.salesAmount = defaultValue(dto.getSalesAmount());
    this.salesCost = defaultValue(dto.getSalesCost());
    this.salesProfit = defaultValue(dto.getSalesProfit());
    this.profitRate = buildProfitRate(this.salesAmount, this.salesProfit);
  }

  private BigDecimal defaultValue(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private String buildProfitRate(BigDecimal amount, BigDecimal profit) {
    if (amount == null || BigDecimal.ZERO.compareTo(amount) == 0) {
      return "0.00%";
    }
    return profit.multiply(new BigDecimal("100")).divide(amount, 2, RoundingMode.HALF_UP) + "%";
  }
}
