package com.lframework.xingyun.sc.impl.sale;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 销售出库查询毛利率计算，百分数保留两位小数。 */
public final class SaleOutProfitCalculator {

    /** 工具类不允许实例化。 */
    private SaleOutProfitCalculator() {
    }

    /** 验收金额非零时优先作为毛利基数，否则使用销售金额。 */
    public static BigDecimal baseAmount(BigDecimal totalAmount, BigDecimal confirmAmt) {
        return confirmAmt != null && confirmAmt.signum() != 0 ? confirmAmt
                : (totalAmount == null ? BigDecimal.ZERO : totalAmount);
    }

    /** 按已汇总利润计算毛利率，基数为零时返回零。 */
    public static BigDecimal rate(BigDecimal profit, BigDecimal baseAmount) {
        if (baseAmount == null || baseAmount.signum() == 0) {
            return new BigDecimal("0.00");
        }
        return (profit == null ? BigDecimal.ZERO : profit).multiply(new BigDecimal("100"))
                .divide(baseAmount, 2, RoundingMode.HALF_UP);
    }
}
