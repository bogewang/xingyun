package com.lframework.xingyun.sc.excel.sale.out;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.annotations.excel.ExcelRequired;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaleOutSheetImportModel implements ExcelModel {
    @ExcelIgnore
    private Integer seq;
    /**
     * 商品名称
     */
    @ExcelRequired
    @ExcelProperty("商品名称")
    private String productName;

    /**
     * 商品ID
     */
    @ExcelIgnore()
    private String productId;

    /**
     * 商品单位关系ID
     */
    @ExcelIgnore
    private String unitId;

    /**
     * 商品编号
     */
    @ExcelIgnore()
    private String productCode;

    /**
     * 原价
     */
    @ExcelIgnore()
    private BigDecimal oriPrice;

    /**
     * 商品规格
     */
    // @ExcelRequired
    @ExcelProperty("规格")
    private String spec;


    /**
     * 商品单位
     */
    @ExcelRequired
    @ExcelProperty("单位")
    private String unit;

    /**
     * 收货数量
     */
    @ExcelProperty("数量")
    private BigDecimal orderNum;

    /**
     * 采购价
     */
    // @ExcelRequired
    @ExcelProperty("单价")
    private BigDecimal taxPrice;

    /**
     * 商品备注
     */
    @ExcelProperty("备注")
    private String description;

    /**
     * 验收数量
     */
    @ExcelProperty("验收数量")
    private BigDecimal confirmNum;
}
