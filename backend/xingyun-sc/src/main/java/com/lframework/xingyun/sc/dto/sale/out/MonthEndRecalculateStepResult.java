package com.lframework.xingyun.sc.dto.sale.out;

import com.lframework.starter.web.core.dto.BaseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 月底成本重算（逐天执行）返回结果
 *
 * @author bogewang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthEndRecalculateStepResult implements BaseDto, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 本次更新的销售出库单数
     */
    private int updatedSheetCount;

    /**
     * 本次更新的明细行数
     */
    private int updatedDetailCount;

    /**
     * 无采购价未填充的商品数
     */
    private int notFilledCount;

    /**
     * 已处理日期
     */
    private LocalDate processedDate;

    /**
     * 是否发生错误
     */
    private boolean hasError;

    /**
     * 错误信息（仅 hasError=true 时有值）
     */
    private String errorMsg;
}
