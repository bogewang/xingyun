package com.lframework.xingyun.sc.excel.purchase.receive;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.annotations.excel.ExcelRequired;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

@Data
public class ReceiveSheetImportModel implements ExcelModel {

  /**
   * 仓库ID
   */
  @ExcelIgnore
  private String scId;

  /**
   * 仓库编号
   */
  // @ExcelRequired
  // @ExcelProperty("仓库编号")
  @ExcelIgnore
  private String scCode;

  /**
   * 供应商ID
   */
  @ExcelIgnore
  private String supplierId;

  /**
   * 供应商编号
   */
  // @ExcelRequired
  // @ExcelProperty("供应商编号")
  @ExcelIgnore
  private String supplierCode;

  /**
   * 采购员ID
   */
  @ExcelIgnore
  private String purchaserId;

  /**
   * 采购员编号
   */
  // @ExcelProperty("采购员编号")
  @ExcelIgnore
  private String purchaserCode;

  /**
   * 付款日期
   */
  // @ExcelRequired
  // @ExcelProperty("付款日期")
  @ExcelIgnore
  private Date paymentDate;

  /**
   * 实际到货日期
   */
  // @ExcelRequired
  // @ExcelProperty("实际到货日期")
  @ExcelIgnore
  private Date receiveDate;

  /**
   * 商品ID
   */
  @ExcelIgnore
  private String productId;

  /**
   * 商品编号
   */
  // @ExcelRequired
  // @ExcelProperty("商品编号")
  @ExcelIgnore
  private String productCode;

  /**
   * 商品名称
   */
  @ExcelRequired
  @ExcelProperty("商品名称")
  private String productName;

  /**
   * 采购价
   */
  @ExcelRequired
  @ExcelProperty("采购价")
  private BigDecimal purchasePrice;

  /**
   * 收货数量
   */
  @ExcelRequired
  @ExcelProperty("收货数量")
  private BigDecimal receiveNum;

  /**
   * 是否赠品
   */
  // @ExcelRequired
  // @ExcelProperty("是否赠品")
  @ExcelIgnore
  private String gift;

  /**
   * 单据明细备注
   */
  // @ExcelProperty("单据明细备注")
  @ExcelIgnore
  private String detailDescription;

  /**
   * 单据备注
   */
  @ExcelProperty("备注")
  private String description;
}
