package com.lframework.xingyun.basedata.entity.quote;

import com.lframework.xingyun.basedata.enums.quote.QuoteSheetStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 报价单领域模型测试。
 */
public class QuoteSheetModelTest {

  /**
   * 验证报价单状态枚举的编码和描述。
   */
  @Test
  public void shouldExposeEnabledAndDisabledStatuses() {

    Assert.assertEquals(QuoteSheetStatus.ENABLED.getCode(), Integer.valueOf(1));
    Assert.assertEquals(QuoteSheetStatus.ENABLED.getDesc(), "启用");
    Assert.assertEquals(QuoteSheetStatus.DISABLED.getCode(), Integer.valueOf(0));
    Assert.assertEquals(QuoteSheetStatus.DISABLED.getDesc(), "停用");
  }

  /**
   * 验证报价单主表和明细的关键字段可读写。
   */
  @Test
  public void shouldReadAndWriteQuoteSheetFields() {

    QuoteSheet quoteSheet = new QuoteSheet();
    quoteSheet.setId("quote-1");
    quoteSheet.setName("夏季报价");
    quoteSheet.setStartDate(LocalDate.of(2026, 8, 29));
    quoteSheet.setEndDate(LocalDate.of(2026, 9, 30));
    quoteSheet.setStatus(QuoteSheetStatus.DISABLED);
    quoteSheet.setDescription("测试报价单");

    QuoteSheetDetail detail = new QuoteSheetDetail();
    detail.setId("detail-1");
    detail.setQuoteSheetId(quoteSheet.getId());
    detail.setProductId("product-1");
    detail.setSalePrice(new BigDecimal("99.90"));

    Assert.assertEquals(quoteSheet.getName(), "夏季报价");
    Assert.assertEquals(quoteSheet.getStatus(), QuoteSheetStatus.DISABLED);
    Assert.assertEquals(detail.getQuoteSheetId(), "quote-1");
    Assert.assertEquals(detail.getSalePrice(), new BigDecimal("99.90"));
  }
}
