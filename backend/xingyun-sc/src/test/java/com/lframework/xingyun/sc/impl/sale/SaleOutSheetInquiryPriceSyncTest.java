package com.lframework.xingyun.sc.impl.sale;

import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 询价商品售价同步计算测试。
 */
class SaleOutSheetInquiryPriceSyncTest {

  /**
   * 验证同步售价后同时重算销售金额、验收金额和利润。
   */
  @Test
  void applyInquirySalePriceShouldRecalculateAmountsAndProfit() {
    SaleOutSheetDetail detail = new SaleOutSheetDetail();
    detail.setBusinessNum(new BigDecimal("5"));
    detail.setOrderNum(new BigDecimal("5"));
    detail.setConfirmNum(new BigDecimal("4"));
    detail.setCostPrice(new BigDecimal("7"));

    SaleOutSheetServiceImpl.applyInquirySalePrice(detail, new BigDecimal("12.345"));

    assertEquals(new BigDecimal("12.345"), detail.getTaxPrice());
    assertEquals(new BigDecimal("61.73"), detail.getTaxAmount());
    assertEquals(new BigDecimal("49.38"), detail.getConfirmAmt());
    assertEquals(0, new BigDecimal("21.38").compareTo(detail.getTotalProfit()));
  }

  /**
   * 验证成本未补全时不生成虚假的明细利润。
   */
  @Test
  void applyInquirySalePriceShouldKeepProfitNullWhenCostMissing() {
    SaleOutSheetDetail detail = new SaleOutSheetDetail();
    detail.setBusinessNum(BigDecimal.ONE);
    detail.setOrderNum(BigDecimal.ONE);
    detail.setConfirmNum(BigDecimal.ONE);

    SaleOutSheetServiceImpl.applyInquirySalePrice(detail, BigDecimal.TEN);

    assertNull(detail.getTotalProfit());
  }
}
