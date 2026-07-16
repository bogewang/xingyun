package com.lframework.xingyun.sc.bo.sale.out;

import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.assertNotNull;

class GetSaleOutSheetBoTest {

  /**
   * 确认销售出库修改接口明细模型包含后端持久化金额字段。
   */
  @Test
  void orderDetailShouldExposeTaxAmount() throws NoSuchFieldException {
    Field taxAmount = GetSaleOutSheetBo.OrderDetailBo.class.getDeclaredField("taxAmount");

    assertNotNull(taxAmount);
  }
}
