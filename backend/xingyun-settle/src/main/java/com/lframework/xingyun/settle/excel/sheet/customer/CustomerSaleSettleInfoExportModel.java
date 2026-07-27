package com.lframework.xingyun.settle.excel.sheet.customer;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.starter.web.core.utils.EnumUtil;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.settle.bo.sheet.customer.CustomerSaleSettleInfoBo;
import com.lframework.xingyun.settle.enums.CustomerSaleSettleBizType;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import lombok.Data;

/**
 * 客户销售结算工作台导出数据模型。
 */
@Data
public class CustomerSaleSettleInfoExportModel implements ExcelModel {

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd HH:mm:ss");

  @ExcelProperty("客户")
  private String customerName;

  @ExcelProperty("销售单/销售退货单")
  private String code;

  @ExcelProperty("单据类型")
  private String bizType;

  @ExcelProperty("单据日期")
  private String orderDate;

  @ExcelProperty("应收")
  private BigDecimal totalAmount;

  @ExcelProperty("已收")
  private BigDecimal receivedAmount;

  @ExcelProperty("对账金额")
  private BigDecimal checkAmount;

  @ExcelProperty("已结算")
  private BigDecimal settleAmount;

  @ExcelProperty("未结算")
  private BigDecimal unSettleAmount;

  @ExcelProperty("状态")
  private String settleStatus;

  @ExcelProperty("对账时间")
  private String checkTime;

  @ExcelProperty("结算时间")
  private String settleTime;

  @ExcelProperty("单据备注")
  private String description;

  @ExcelProperty("对账备注")
  private String checkDescription;

  @ExcelProperty("结算备注")
  private String settleDescription;

  /**
   * 根据工作台数据创建导出数据。
   *
   * @param data 工作台数据
   */
  public CustomerSaleSettleInfoExportModel(CustomerSaleSettleInfoBo data) {
    this.customerName = data.getCustomerName();
    this.code = data.getCode();
    this.bizType = EnumUtil.getDesc(CustomerSaleSettleBizType.class, data.getBizType());
    this.orderDate = data.getOrderDate() == null ? null : data.getOrderDate().toString();
    this.totalAmount = data.getTotalAmount();
    this.receivedAmount = data.getReceivedAmount();
    this.checkAmount = data.getCheckAmount();
    this.settleAmount = data.getSettleAmount();
    this.unSettleAmount = data.getUnSettleAmount();
    this.settleStatus = EnumUtil.getDesc(SettleStatus.class, data.getSettleStatus());
    this.checkTime = data.getCheckTime() == null ? null : DATE_TIME_FORMATTER.format(
        data.getCheckTime());
    this.settleTime = data.getSettleTime() == null ? null : DATE_TIME_FORMATTER.format(
        data.getSettleTime());
    this.description = data.getDescription();
    this.checkDescription = data.getCheckDescription();
    this.settleDescription = data.getSettleDescription();
  }
}
