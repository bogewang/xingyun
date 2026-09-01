package com.lframework.xingyun.sc.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** 销售出库报价单来源字段归属测试。 */
class SaleOutSheetQuoteSheetIdTest {
  /** 报价单来源应保存于销售出库主表而非明细。 */
  @Test
  void shouldStoreQuoteSheetIdOnSaleOutSheetOnly() throws Exception {
    SaleOutSheet sheet = new SaleOutSheet();
    sheet.setQuoteSheetId("quote-1");
    assertEquals("quote-1", sheet.getQuoteSheetId());
    assertThrows(NoSuchFieldException.class,
        () -> SaleOutSheetDetail.class.getDeclaredField("quoteSheetId"));
  }
}
