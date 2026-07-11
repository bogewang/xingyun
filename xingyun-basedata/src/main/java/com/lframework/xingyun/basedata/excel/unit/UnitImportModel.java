package com.lframework.xingyun.basedata.excel.unit;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.annotations.excel.ExcelRequired;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import lombok.Data;

@Data
public class UnitImportModel implements ExcelModel {
    @ExcelRequired
    @ExcelProperty("名称")
    private String name;

    @ExcelProperty("备注")
    private String description;
}
