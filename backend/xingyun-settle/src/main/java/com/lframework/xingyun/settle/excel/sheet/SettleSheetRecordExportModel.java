package com.lframework.xingyun.settle.excel.sheet;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.google.common.collect.Lists;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.utils.DateUtil;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.entity.Supplier;
import com.lframework.xingyun.basedata.service.supplier.SupplierService;
import com.lframework.xingyun.sc.entity.ReceiveSheet;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetService;
import com.lframework.xingyun.settle.entity.SettleSheet;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.stream.Collectors;

@Data
public class SettleSheetRecordExportModel extends BaseBo<SettleSheet> implements ExcelModel {

  @ExcelProperty("结算单号")
  private String code;

  @ExcelProperty("结算时间")
  @DateTimeFormat(StringPool.DATE_TIME_PATTERN)
  private Date recordTime;

  @ExcelProperty("供应商名称")
  private String supplierName;

  @ExcelProperty("货单号")
  private String bizSheetIdCodes;

  @ExcelProperty("对账金额")
  private BigDecimal totalCheckAmt;

  @ExcelProperty("累计已付")
  private BigDecimal totalPaidAmt;

  @ExcelProperty("结算金额")
  private BigDecimal actualSettleAmount;

  @ExcelProperty("备注")
  private String description;

  @ExcelProperty("操作人")
  private String createBy;

  public SettleSheetRecordExportModel() {
  }

  public SettleSheetRecordExportModel(SettleSheet dto) {
    super(dto);
  }

  @Override
  public <A> BaseBo<SettleSheet> convert(SettleSheet dto) {
    return this;
  }

  @Override
  protected void afterInit(SettleSheet dto) {
    SupplierService supplierService = ApplicationUtil.getBean(SupplierService.class);
    Supplier supplier = supplierService.findById(dto.getSupplierId());

    ReceiveSheetService receiveSheetService = ApplicationUtil.getBean(ReceiveSheetService.class);
    String bizCodes = receiveSheetService.selectByIds(Lists.newArrayList(dto.getBizSheetIds().split(",")))
        .stream()
        .map(ReceiveSheet::getCode)
        .collect(Collectors.joining(","));

    this.setCode(dto.getCode());
    this.setRecordTime(DateUtil.toDate(dto.getCreateTime()));
    this.setSupplierName(supplier == null ? null : supplier.getName());
    this.setBizSheetIdCodes(bizCodes);
    this.setTotalCheckAmt(dto.getTotalCheckAmt());
    this.setTotalPaidAmt(NumberUtil.sub(
        dto.getTotalCheckAmt() == null ? BigDecimal.ZERO : dto.getTotalCheckAmt(),
        dto.getTotalUnSettleAmt() == null ? BigDecimal.ZERO : dto.getTotalUnSettleAmt()));
    this.setActualSettleAmount(dto.getTotalAmount());
    this.setDescription(dto.getDescription());
    this.setCreateBy(dto.getCreateBy());
  }
}
