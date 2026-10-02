package com.lframework.xingyun.sc.impl.sale;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SaleOutProfitCalculatorTest {

    /** 截图单据毛利率为6.01%，不能按未补全成本重算利润。 */
    @Test
    void shouldCalculateScreenshotRate() {
        assertEquals(new BigDecimal("6.01"), SaleOutProfitCalculator.rate(
                new BigDecimal("683.10"), new BigDecimal("11368.02")));
    }

    /** 验收金额优先，零值或缺失时回退销售金额。 */
    @Test
    void shouldResolveBaseAmount() {
        assertEquals(new BigDecimal("180"), SaleOutProfitCalculator.baseAmount(
                new BigDecimal("200"), new BigDecimal("180")));
        assertEquals(new BigDecimal("100"), SaleOutProfitCalculator.baseAmount(
                new BigDecimal("100"), BigDecimal.ZERO));
        assertEquals(new BigDecimal("100"), SaleOutProfitCalculator.baseAmount(
                new BigDecimal("100"), null));
        assertEquals(BigDecimal.ZERO, SaleOutProfitCalculator.baseAmount(null, null));
    }

    /** 支持负利润、空利润、零分母和四舍五入。 */
    @Test
    void shouldHandleBoundaryValues() {
        assertEquals(new BigDecimal("-12.89"), SaleOutProfitCalculator.rate(
                new BigDecimal("-4.11"), new BigDecimal("31.89")));
        assertEquals(new BigDecimal("0.00"), SaleOutProfitCalculator.rate(null, BigDecimal.TEN));
        assertEquals(new BigDecimal("0.00"), SaleOutProfitCalculator.rate(BigDecimal.TEN, BigDecimal.ZERO));
        assertEquals(new BigDecimal("33.33"), SaleOutProfitCalculator.rate(BigDecimal.ONE, new BigDecimal("3")));
    }
}
