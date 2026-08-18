package com.lframework.xingyun.sc.excel.sale.out;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售出库开票明细导出行。
 */
@Data
@AllArgsConstructor
public class SaleOutSheetInvoiceDetailExportModel implements ExcelModel {

    @ExcelProperty("商品编号")
    private String productCode;

    @ExcelProperty("商品名称")
    private String productName;

    @ExcelProperty("规格")
    private String spec;

    @ExcelProperty("商品分类")
    private String categoryName;

    @ExcelProperty("单位")
    private String unit;

    @ExcelProperty("数量")
    private BigDecimal quantity;

    @ExcelProperty("金额")
    private BigDecimal amount;
}
