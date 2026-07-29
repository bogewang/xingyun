package com.lframework.xingyun.sc.bo.sale.out;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class QuerySaleOutSheetBoTest {

  /** 验证未付金额优先按验收金额计算。 */
  @Test
  void shouldUseConfirmAmountWhenCalculatingUnpaidAmount() {
    assertEquals(new BigDecimal("38.05"), QuerySaleOutSheetBo.calculateUnpaidAmount(
        new BigDecimal("7.61"), new BigDecimal("38.05"), BigDecimal.ZERO));
  }

  /** 验证验收金额为零时未付金额回退销售金额。 */
  @Test
  void shouldFallbackToTotalAmountWhenConfirmAmountIsZero() {
    assertEquals(new BigDecimal("6.61"), QuerySaleOutSheetBo.calculateUnpaidAmount(
        new BigDecimal("7.61"), BigDecimal.ZERO, BigDecimal.ONE));
  }
}
