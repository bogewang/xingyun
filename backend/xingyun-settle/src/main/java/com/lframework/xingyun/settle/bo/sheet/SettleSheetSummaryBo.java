package com.lframework.xingyun.settle.bo.sheet;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 供应商结算汇总结果
 */
@Data
public class SettleSheetSummaryBo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String supplierId;

    private String supplierCode;

    private String supplierName;
    /**
     * 未对账单数量
     */
    private Integer unCheckSheetNum = 0;
    /**
     * 未对账单总金额
     */
    private BigDecimal unCheckTotalAmount = BigDecimal.ZERO;
    /**
     * 未结算单数量
     */
    private Integer unSettleSheetNum = 0;
    /**
     * 未结算单总金额
     */
    private BigDecimal unSettleTotalAmount = BigDecimal.ZERO;
    /**
     * 部份结算单数量
     */
    private Integer partSettleSheetNum = 0;
    /**
     * 部份结算单总金额
     */
    private BigDecimal partSettleTotalAmount = BigDecimal.ZERO;
    /**
     * 已结算单数量
     */
    private Integer settledSheetNum = 0;
    /**
     * 已结算单总金额
     */
    private BigDecimal settledTotalAmount = BigDecimal.ZERO;
}
