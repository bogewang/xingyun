package com.lframework.xingyun.sc.excel.sale.out;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.annotations.excel.ExcelRequired;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import lombok.Data;

@Data
public class SaleOutSheetQueryImportModel extends SaleOutSheetImportModel implements ExcelModel {

    @ExcelProperty("配送日期")
    private String actualDate;

    @ExcelRequired
    @ExcelProperty("销售日期")
    private String orderDate;

    @ExcelRequired
    @ExcelProperty("客户")
    private String customerName;

}
