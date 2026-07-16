package com.lframework.xingyun.basedata.excel.unit;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.xingyun.basedata.entity.Unit;
import lombok.Data;

@Data
public class UnitExportModel implements ExcelModel {

    @ExcelProperty("编码")
    private String code;

    @ExcelProperty("名称")
    private String name;

    @ExcelProperty("备注")
    private String description;

    public UnitExportModel() {
    }

    public UnitExportModel(Unit dto) {
        this.code = dto.getCode();
        this.name = dto.getName();
        this.description = dto.getDescription();
    }
}
