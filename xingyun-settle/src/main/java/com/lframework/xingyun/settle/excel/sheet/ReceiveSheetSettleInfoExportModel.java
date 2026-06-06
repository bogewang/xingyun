package com.lframework.xingyun.settle.excel.sheet;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.utils.DateUtil;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.starter.web.core.utils.EnumUtil;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.settle.bo.sheet.ReceiveSheetSettleInfoBo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ReceiveSheetSettleInfoExportModel implements ExcelModel {

  @ExcelProperty("供应商名称")
  private String supplierName;

  @ExcelProperty("货流单号")
  private String code;

  @ExcelProperty("下单时间")
  @DateTimeFormat(StringPool.DATE_PATTERN)
  private Date orderDate;

  @ExcelProperty("货单类型")
  private String bizType;

  @ExcelProperty("商品数量")
  private BigDecimal totalNum;

  @ExcelProperty("货流单金额")
  private BigDecimal totalAmount;

  @ExcelProperty("本单未结算")
  private BigDecimal unSettleAmount;

  @ExcelProperty("对账金额")
  private BigDecimal checkAmount;

  @ExcelProperty("结算金额")
  private BigDecimal settleAmount;

  @ExcelProperty("状态")
  private String settleStatus;

  @ExcelProperty("对账时间")
  @DateTimeFormat(StringPool.DATE_TIME_PATTERN)
  private Date checkTime;

  @ExcelProperty("结算时间")
  @DateTimeFormat(StringPool.DATE_TIME_PATTERN)
  private Date settleTime;

  @ExcelProperty("货流备注")
  private String description;

  @ExcelProperty("对账备注")
  private String checkDescription;

  @ExcelProperty("结算备注")
  private String settleDescription;

  public ReceiveSheetSettleInfoExportModel() {
  }

  public ReceiveSheetSettleInfoExportModel(ReceiveSheetSettleInfoBo dto) {
    this.setSupplierName(dto.getSupplierName());
    this.setCode(dto.getCode());
    if (dto.getOrderDate() != null) {
      this.setOrderDate(DateUtil.toDate(dto.getOrderDate()));
    }
    this.setBizType("进货单");
    this.setTotalNum(dto.getTotalNum());
    this.setTotalAmount(dto.getTotalAmount());
    this.setUnSettleAmount(dto.getUnSettleAmount());
    this.setCheckAmount(dto.getCheckAmount());
    this.setSettleAmount(dto.getSettleAmount());
    this.setSettleStatus(EnumUtil.getDesc(SettleStatus.class, dto.getSettleStatus()));
    if (dto.getCheckTime() != null) {
      this.setCheckTime(DateUtil.toDate(dto.getCheckTime()));
    }
    if (dto.getSettleTime() != null) {
      this.setSettleTime(DateUtil.toDate(dto.getSettleTime()));
    }
    this.setDescription(dto.getDescription());
    this.setCheckDescription(dto.getCheckDescription());
    this.setSettleDescription(dto.getSettleDescription());
  }
}
