package com.lframework.xingyun.basedata.excel.quote;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.ExcelIgnore;
import com.lframework.starter.web.core.components.excel.ExcelModel;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 报价单导入商品行。
 */
@Data
public class QuoteSheetImportModel implements ExcelModel {
    @ExcelIgnore
    private Integer seq;

    @ExcelProperty("商品名称")
    private String name;

    @ExcelProperty("规格")
    private String spec;

    @ExcelProperty("单位")
    private String unit;

    // @ExcelProperty("单位ID")
    // private String unitId;

    @ExcelProperty("销售单价")
    private BigDecimal salePrice;

    @ExcelIgnore
    private String productId;

    @ExcelIgnore
    private String code;
}
