package com.lframework.xingyun.sc.excel.sale.out;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.utils.DateUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.inner.entity.SysUser;
import com.lframework.starter.web.inner.service.system.SysUserService;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.sc.entity.SaleOrder;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.service.sale.SaleOrderService;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SaleOutSheetExportModel extends BaseBo<SaleOutSheet> implements ExcelModel {

  /**
   * 单号
   */
  @ExcelProperty("业务单据号")
  private String code;

  /**
   * 仓库编号
   */
  @ExcelProperty("仓库编号")
  private String scCode;

  /**
   * 仓库名称
   */
  @ExcelProperty("仓库名称")
  private String scName;

  /**
   * 客户编号
   */
  @ExcelProperty("客户编号")
  private String customerCode;

  /**
   * 客户名称
   */
  @ExcelProperty("客户名称")
  private String customerName;

  /**
   * 销售员姓名
   */
  @ExcelProperty("销售员")
  private String salerName;

  /**
   * 订单日期
   */
  @ExcelProperty("订单日期")
  @DateTimeFormat(StringPool.DATE_PATTERN)
  private Date orderDate;

  /**
   * 单据总金额
   */
  @ExcelProperty("单据总金额")
  private BigDecimal totalAmount;

  /**
   * 已付金额
   */
  @ExcelProperty("已付金额")
  private BigDecimal paidAmount;

  /**
   * 未付金额
   */
  @ExcelProperty("未付金额")
  private BigDecimal unpaidAmount;

  /**
   * 总利润
   */
  @ExcelProperty("总利润")
  private BigDecimal totalProfit;

  /**
   * 商品数量
   */
  @ExcelProperty("商品数量")
  private BigDecimal receiveNum;

  /**
   * 赠品数量
   */
  @ExcelProperty("赠品数量")
  private BigDecimal giftNum;

  /**
   * 操作时间
   */
  @ExcelProperty("操作时间")
  @DateTimeFormat(StringPool.DATE_TIME_PATTERN)
  private Date createTime;

  /**
   * 操作人
   */
  @ExcelProperty("操作人")
  private String createBy;

  /**
   * 审核状态
   */
  @ExcelProperty("审核状态")
  private String status;

  /**
   * 审核时间
   */
  @ExcelProperty("审核时间")
  @DateTimeFormat(StringPool.DATE_TIME_PATTERN)
  private Date approveTime;

  /**
   * 审核人
   */
  @ExcelProperty("审核人")
  private String approveBy;

  /**
   * 结算状态
   */
  @ExcelProperty("结算状态")
  private String settleStatus;

  /**
   * 备注
   */
  @ExcelProperty("备注")
  private String description;

  /**
   * 采购订单号
   */
  @ExcelProperty("销售订单号")
  private String purchaseOrderCode;

  public SaleOutSheetExportModel() {

  }

  public SaleOutSheetExportModel(SaleOutSheet dto) {

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
    SysUser saler = null;
    if (!StringUtil.isBlank(dto.getSalerId())) {
      saler = userService.findById(dto.getSalerId());
    }
    SysUser approveBy = null;
    if (!StringUtil.isBlank(dto.getApproveBy())) {
      approveBy = userService.findById(dto.getApproveBy());
    }

    this.setCode(dto.getCode());
    this.setCustomerCode(customer.getCode());
    this.setCustomerName(customer.getName());
    this.setSalerName(saler == null ? null : saler.getName());
    if (dto.getOrderDate() != null) {
      this.setOrderDate(DateUtil.toDate(dto.getOrderDate()));
    }
    this.setTotalAmount(dto.getTotalAmount());
    BigDecimal paidAmount = dto.getPaidAmount() == null ? BigDecimal.ZERO : dto.getPaidAmount();
    this.setPaidAmount(paidAmount);
    this.setUnpaidAmount(dto.getTotalAmount() == null ? BigDecimal.ZERO : dto.getTotalAmount().subtract(paidAmount));
    this.setTotalProfit(dto.getTotalProfit());
    this.setReceiveNum(dto.getTotalNum());
    this.setGiftNum(dto.getTotalGiftNum());
    this.setCreateTime(DateUtil.toDate(dto.getCreateTime()));
    this.setStatus(dto.getStatus().getDesc());
    if (dto.getApproveTime() != null) {
      this.setApproveTime(DateUtil.toDate(dto.getApproveTime()));
    }
    if (approveBy != null) {
      this.setApproveBy(approveBy.getName());
    }
    this.setSettleStatus(dto.getSettleStatus().getDesc());
    this.setDescription(dto.getDescription());
    if (!StringUtil.isBlank(dto.getSaleOrderId())) {
      SaleOrderService saleOrderService = ApplicationUtil.getBean(SaleOrderService.class);
      SaleOrder saleOrder = saleOrderService.getById(dto.getSaleOrderId());
      this.setPurchaseOrderCode(saleOrder.getCode());
    }
  }
}
