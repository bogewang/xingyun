package com.lframework.xingyun.sc.excel.sale.out;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.utils.DateUtil;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.inner.service.system.SysUserService;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import lombok.Data;

@Data
public class SaleOutSheetProfitExportModel extends BaseBo<SaleOutSheet> implements ExcelModel {

  @ExcelProperty("单据日期")
  @DateTimeFormat(StringPool.DATE_PATTERN)
  private Date orderDate;

  @ExcelProperty("单据编号")
  private String code;

  @ExcelProperty("客户")
  private String customerName;

  @ExcelProperty("销售收入")
  private BigDecimal salesAmount;

  @ExcelProperty("销售成本")
  private BigDecimal salesCost;

  @ExcelProperty("销售毛利")
  private BigDecimal salesProfit;

  @ExcelProperty("毛利率")
  private String profitRate;

  @ExcelProperty("实收金额")
  private BigDecimal paidAmount;

  @ExcelProperty("其他费用")
  private BigDecimal otherFee;

  @ExcelProperty("销售员")
  private String salerName;

  public SaleOutSheetProfitExportModel() {
  }

  public SaleOutSheetProfitExportModel(SaleOutSheet dto) {
    super(dto);
  }

  @Override
  public <A> BaseBo<SaleOutSheet> convert(SaleOutSheet dto) {
    return this;
  }

  @Override
  protected void afterInit(SaleOutSheet dto) {
    CustomerService customerService = ApplicationUtil.getBean(CustomerService.class);
    Customer customer = customerService.findById(dto.getCustomerId());
    SysUserService userService = ApplicationUtil.getBean(SysUserService.class);

    if (dto.getOrderDate() != null) {
      this.orderDate = DateUtil.toDate(dto.getOrderDate());
    }
    this.code = dto.getCode();
    this.customerName = customer == null ? null : customer.getName();
    this.salesAmount = defaultValue(dto.getTotalAmount());
    this.salesProfit = defaultValue(dto.getTotalProfit());
    this.salesCost = NumberUtil.sub(this.salesAmount, this.salesProfit);
    this.profitRate = buildProfitRate(this.salesAmount, this.salesProfit);
    this.paidAmount = defaultValue(dto.getPaidAmount());
    this.otherFee = BigDecimal.ZERO;
    if (dto.getSalerId() != null) {
      this.salerName = userService.findById(dto.getSalerId()).getName();
    }
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
