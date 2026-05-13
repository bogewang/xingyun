package com.lframework.xingyun.sc.excel.purchase;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.annotations.excel.ExcelRequired;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import lombok.Data;

@Data
public class PurchaseOrderQueryImportModel extends PurchaseOrderImportModel implements ExcelModel {
  @ExcelRequired
  @ExcelProperty("单据日期")
  private String orderDate;

  @ExcelRequired
  @ExcelProperty("供应商")
  private String supplierName;

}
