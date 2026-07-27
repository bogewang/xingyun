package com.lframework.xingyun.settle.excel.sheet.customer;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.xingyun.settle.bo.sheet.customer.CustomerSettleOverviewBo;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 客户结算总览导出数据模型。
 */
@Data
public class CustomerSettleOverviewExportModel implements ExcelModel {

  @ExcelProperty("客户ID")
  private String customerId;

  @ExcelProperty("客户编号")
  private String customerCode;

  @ExcelProperty("客户名称")
  private String customerName;

  @ExcelProperty("待对账单据数")
  private Integer unCheckCount;

  @ExcelProperty("待对账金额")
  private BigDecimal unCheckAmount;

  @ExcelProperty("待结算单据数")
  private Integer unSettleCount;

  @ExcelProperty("待结算金额")
  private BigDecimal unSettleAmount;

  @ExcelProperty("部分结算单据数")
  private Integer partSettleCount;

  @ExcelProperty("部分结算金额")
  private BigDecimal partSettleAmount;

  @ExcelProperty("已结算单据数")
  private Integer settledCount;

  @ExcelProperty("已结算金额")
  private BigDecimal settledAmount;

  /**
   * 根据客户结算总览创建导出模型。
   *
   * @param data 客户结算总览数据
   */
  public CustomerSettleOverviewExportModel(CustomerSettleOverviewBo data) {
    this.customerId = data.getCustomerId();
    this.customerCode = data.getCustomerCode();
    this.customerName = data.getCustomerName();
    this.unCheckCount = data.getUnCheckCount();
    this.unCheckAmount = data.getUnCheckAmount();
    this.unSettleCount = data.getUnSettleCount();
    this.unSettleAmount = data.getUnSettleAmount();
    this.partSettleCount = data.getPartSettleCount();
    this.partSettleAmount = data.getPartSettleAmount();
    this.settledCount = data.getSettledCount();
    this.settledAmount = data.getSettledAmount();
  }
}
