package com.lframework.xingyun.basedata.excel.quote;

import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** 报价单商品明细 Excel 行数据测试。 */
class QuoteSheetDetailExportModelTest {

  /** 应完整保留报价单和商品明细字段。 */
  @Test
  void shouldCreateExportModelFromDetailData() {
    QuoteSheetDetailExportDto data = new QuoteSheetDetailExportDto();
    data.setQuoteSheetName("九月报价单");
    data.setStartDate("2026-09-01");
    data.setEndDate("2026-09-30");
    data.setStatus("启用");
    data.setProductCode("P001");
    data.setProductName("测试商品");
    data.setShortName("商品");
    data.setSpec("500g");
    data.setUnit("箱");
    data.setSalePrice(new BigDecimal("12.50"));
    data.setDescription("测试备注");

    QuoteSheetDetailExportModel result = new QuoteSheetDetailExportModel(data);

    Assertions.assertEquals("九月报价单", result.getQuoteSheetName());
    Assertions.assertEquals("P001", result.getProductCode());
    Assertions.assertEquals("箱", result.getUnit());
    Assertions.assertEquals(new BigDecimal("12.50"), result.getSalePrice());
  }

  /** 应将商品保存的单位 ID 转换为单位字典名称。 */
  @Test
  void shouldResolveUnitNameFromUnitDictionary() {
    Assertions.assertEquals("箱", QuoteSheetDetailExportTaskWorker.resolveUnitName("unit-1",
        Collections.singletonMap("unit-1", "箱")));
    Assertions.assertEquals("历史单位", QuoteSheetDetailExportTaskWorker.resolveUnitName("历史单位",
        Collections.emptyMap()));
  }
}
