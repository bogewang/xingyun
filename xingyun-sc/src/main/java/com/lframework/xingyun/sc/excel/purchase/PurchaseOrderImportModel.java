package com.lframework.xingyun.sc.excel.purchase;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.annotations.excel.ExcelRequired;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderImportModel implements ExcelModel {

  /**
   * 商品ID
   */
  @ExcelIgnore
  private String productId;

  /**
   * 商品编号
   */
  @ExcelIgnore
  private String productCode;

  @ExcelRequired
  @ExcelProperty("商品名称")
  private String productName;
  /**
   * 商品规格
   */
  @ExcelProperty("规格")
  private String spec;


  /**
   * 商品单位
   */
  @ExcelRequired
  @ExcelProperty("单位")
  private String unit;
  /**
   * 采购数量
   */
  @ExcelRequired
  @ExcelProperty("采购数量")
  private BigDecimal purchaseNum;
  /**
   * 采购价
   */
  @ExcelRequired
  @ExcelProperty("采购价")
  private BigDecimal purchasePrice;



  /**
   * 备注
   */
  @ExcelProperty("备注")
  private String description;
}
