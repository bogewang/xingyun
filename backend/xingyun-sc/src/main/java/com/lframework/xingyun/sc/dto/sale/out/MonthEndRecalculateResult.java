package com.lframework.xingyun.sc.dto.sale.out;

import com.lframework.starter.web.core.dto.BaseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 月底成本重算结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthEndRecalculateResult implements BaseDto, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 更新的销售出库单数
     */
    private int updatedSheetCount;

    /**
     * 更新的明细行数
     */
    private int updatedDetailCount;

    /**
     * 无采购价未填充的商品数
     */
    private int notFilledCount;
}
