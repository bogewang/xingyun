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

    /** Excel 中填写“是”或“否”；留空时按“是”处理。 */
    @ExcelProperty("是否询价商品")
    private String inquiryProductText;

    /** 解析后的是否询价商品标识。 */
    @ExcelIgnore
    private Boolean inquiryProduct;

    @ExcelIgnore
    private String productId;

    @ExcelIgnore
    private String code;
}
