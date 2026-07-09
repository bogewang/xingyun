package com.lframework.xingyun.basedata.bo.print;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模板字段说明
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PrintTemplateColumnDescription {
    /**
     * 列名
     */
    private String columnName;
    /**
     * 说明
     */
    private String description;
    /**
     * 示例
     */
    private String demo;
}
