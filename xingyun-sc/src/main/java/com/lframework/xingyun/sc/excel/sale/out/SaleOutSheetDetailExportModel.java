package com.lframework.xingyun.sc.excel.sale.out;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.xingyun.sc.dto.sale.out.QuerySaleOutSheetDetailDto;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaleOutSheetDetailExportModel extends BaseBo<QuerySaleOutSheetDetailDto> implements ExcelModel {

  @ExcelProperty("订单日期")
  private String orderDate;

  @ExcelProperty("客户名称")
  private String customerName;

  @ExcelProperty("商品编号")
  private String productCode;

  @ExcelProperty("名称")
  private String productName;

  @ExcelProperty("简称")
  private String shortName;

  @ExcelProperty("规格")
  private String spec;

  @ExcelProperty("单位")
  private String unit;

  @ExcelProperty("商品分类")
  private String categoryName;

  @ExcelProperty("价格（元）")
  private BigDecimal taxPrice;

  @ExcelProperty("出库数量")
  private BigDecimal orderNum;

  @ExcelProperty("含税金额")
  private BigDecimal taxAmount;

  @ExcelProperty("备注")
  private String description;

  public SaleOutSheetDetailExportModel() {
  }

  public SaleOutSheetDetailExportModel(QuerySaleOutSheetDetailDto dto) {

    super(dto);
  }

  @Override
  public <A> BaseBo<QuerySaleOutSheetDetailDto> convert(QuerySaleOutSheetDetailDto dto) {

    return this;
  }

  @Override
  protected void afterInit(QuerySaleOutSheetDetailDto dto) {

    // 补齐订单日期和客户名称
    this.setOrderDate(dto.getOrderDate());
    this.setCustomerName(dto.getCustomerName());

    this.setProductCode(dto.getProductCode());
    this.setProductName(dto.getProductName());
    this.setShortName(dto.getProductName());
    this.setSpec(dto.getSpec());
    this.setUnit(dto.getUnit());
    this.setCategoryName(dto.getCategoryName());
    this.setTaxPrice(dto.getTaxPrice());
    this.setOrderNum(dto.getOrderNum());
    this.setTaxAmount(dto.getTaxAmount());
    this.setDescription(dto.getDescription());
  }
}
