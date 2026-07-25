package com.lframework.xingyun.settle.excel.sheet.customer;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.starter.web.core.utils.EnumUtil;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.settle.bo.sheet.customer.CustomerSaleSettleInfoBo;
import com.lframework.xingyun.settle.enums.CustomerSettleCheckSheetBizType;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 客户销售结算工作台导出数据模型。
 */
@Data
public class CustomerSaleSettleInfoExportModel implements ExcelModel {

  @ExcelProperty("客户名称")
  private String customerName;

  @ExcelProperty("单号")
  private String code;

  @ExcelProperty("业务类型")
  private String bizType;

  @ExcelProperty("单据金额")
  private BigDecimal totalAmount;

  @ExcelProperty("已收金额")
  private BigDecimal receivedAmount;

  @ExcelProperty("已结算金额")
  private BigDecimal settleAmount;

  @ExcelProperty("未结算金额")
  private BigDecimal unSettleAmount;

  @ExcelProperty("结算状态")
  private String settleStatus;

  /**
   * 根据工作台数据创建导出数据。
   *
   * @param data 工作台数据
   */
  public CustomerSaleSettleInfoExportModel(CustomerSaleSettleInfoBo data) {
    this.customerName = data.getCustomerName();
    this.code = data.getCode();
    this.bizType = EnumUtil.getDesc(CustomerSettleCheckSheetBizType.class, data.getBizType());
    this.totalAmount = data.getTotalAmount();
    this.receivedAmount = data.getReceivedAmount();
    this.settleAmount = data.getSettleAmount();
    this.unSettleAmount = data.getUnSettleAmount();
    this.settleStatus = EnumUtil.getDesc(SettleStatus.class, data.getSettleStatus());
  }
}
