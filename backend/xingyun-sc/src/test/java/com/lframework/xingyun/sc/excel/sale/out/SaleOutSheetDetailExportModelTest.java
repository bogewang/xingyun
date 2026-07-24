package com.lframework.xingyun.sc.excel.sale.out;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 销售出库明细导出模型测试。
 */
class SaleOutSheetDetailExportModelTest {

  /**
   * 验证询价商品标识为空时导出“否”。
   */
  @Test
  void shouldFormatNullInquiryProductForDetailExport() {
    Assertions.assertEquals("否", SaleOutSheetDetailExportModel.formatInquiryProduct(null));
  }
}
