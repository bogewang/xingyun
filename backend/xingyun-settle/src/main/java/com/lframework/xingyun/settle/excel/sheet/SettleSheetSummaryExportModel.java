package com.lframework.xingyun.settle.excel.sheet;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.xingyun.settle.bo.sheet.SettleSheetSummaryBo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SettleSheetSummaryExportModel implements ExcelModel {

  @ExcelProperty("供应商名称")
  private String supplierName;

  @ExcelProperty("待对账单据数")
  private Integer unCheckSheetNum;

  @ExcelProperty("待对账金额")
  private BigDecimal unCheckTotalAmount;

  @ExcelProperty("待结算单据数")
  private Integer unSettleSheetNum;

  @ExcelProperty("待结算金额")
  private BigDecimal unSettleTotalAmount;

  @ExcelProperty("部分结算单据数")
  private Integer partSettleSheetNum;

  @ExcelProperty("部分结算金额")
  private BigDecimal partSettleTotalAmount;

  @ExcelProperty("已结算单据数")
  private Integer settledSheetNum;

  @ExcelProperty("已结算金额")
  private BigDecimal settledTotalAmount;

  public SettleSheetSummaryExportModel() {
  }

  public SettleSheetSummaryExportModel(SettleSheetSummaryBo dto) {
    this.setSupplierName(dto.getSupplierName());
    this.setUnCheckSheetNum(dto.getUnCheckSheetNum());
    this.setUnCheckTotalAmount(dto.getUnCheckTotalAmount());
    this.setUnSettleSheetNum(dto.getUnSettleSheetNum());
    this.setUnSettleTotalAmount(dto.getUnSettleTotalAmount());
    this.setPartSettleSheetNum(dto.getPartSettleSheetNum());
    this.setPartSettleTotalAmount(dto.getPartSettleTotalAmount());
    this.setSettledSheetNum(dto.getSettledSheetNum());
    this.setSettledTotalAmount(dto.getSettledTotalAmount());
  }
}
