package com.lframework.xingyun.sc.dto.stock;

import com.lframework.starter.web.core.dto.BaseDto;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductStockPendingCostResolveDto implements BaseDto, Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 已结算数量
     */
    private BigDecimal settledNum;
    /**
     * 已结算金额
     */
    private BigDecimal settledTaxAmount;
    /**
     * 未结算数量
     */
    private BigDecimal remainNum;
    /**
     * 未结算金额
     */
    private BigDecimal remainTaxAmount;
}
