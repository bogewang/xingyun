package com.lframework.xingyun.basedata.excel.quote;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import java.math.BigDecimal;
import lombok.Data;

/** 报价单商品明细 Excel 行数据。 */
@Data
public class QuoteSheetDetailExportModel implements ExcelModel {
  @ExcelProperty("报价单名称")
  private String quoteSheetName;

  @ExcelProperty("生效开始日期")
  private String startDate;

  @ExcelProperty("生效结束日期")
  private String endDate;

  @ExcelProperty("状态")
  private String status;

  @ExcelProperty("商品编号")
  private String productCode;

  @ExcelProperty("商品名称")
  private String productName;

  @ExcelProperty("规格")
  private String spec;

  @ExcelProperty("单位")
  private String unit;

  @ExcelProperty("销售单价（元）")
  private BigDecimal salePrice;

  @ExcelProperty("备注")
  private String description;

  /** 创建空的 Excel 行数据。 */
  public QuoteSheetDetailExportModel() {
  }

  /**
   * 根据报价单商品明细导出数据创建 Excel 行数据。
   *
   * @param data 报价单商品明细导出数据
   */
  public QuoteSheetDetailExportModel(QuoteSheetDetailExportDto data) {
    this.quoteSheetName = data.getQuoteSheetName();
    this.startDate = data.getStartDate();
    this.endDate = data.getEndDate();
    this.status = data.getStatus();
    this.productCode = data.getProductCode();
    this.productName = data.getProductName();
    this.shortName = data.getShortName();
    this.spec = data.getSpec();
    this.unit = data.getUnit();
    this.salePrice = data.getSalePrice();
    this.description = data.getDescription();
  }
}
