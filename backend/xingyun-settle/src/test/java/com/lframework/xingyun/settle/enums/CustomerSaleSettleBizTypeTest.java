package com.lframework.xingyun.settle.enums;

import org.junit.Assert;
import org.junit.Test;

/**
 * 客户销售结算业务类型测试。
 */
public class CustomerSaleSettleBizTypeTest {

  /**
   * 仅保留销售出库和销售退货两类可结算业务。
   */
  @Test
  public void shouldContainOnlySaleOutAndSaleReturnTypes() {

    Assert.assertEquals(2, CustomerSaleSettleBizType.values().length);
    Assert.assertEquals(Integer.valueOf(1), CustomerSaleSettleBizType.OUT_SHEET.getCode());
    Assert.assertEquals(Integer.valueOf(2), CustomerSaleSettleBizType.SALE_RETURN.getCode());
    Assert.assertEquals("销售出库单", CustomerSaleSettleBizType.OUT_SHEET.getDesc());
    Assert.assertEquals("销售退单", CustomerSaleSettleBizType.SALE_RETURN.getDesc());
  }
}
