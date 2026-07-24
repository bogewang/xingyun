package com.lframework.xingyun.sc.excel.purchase.receive;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 采购收货明细导出模型测试。
 */
class ReceiveSheetDetailExportModelTest {

  /**
   * 验证询价商品标识为真时导出“是”。
   */
  @Test
  void shouldFormatTrueInquiryProductForDetailExport() {
    Assertions.assertEquals("是", ReceiveSheetDetailExportModel.formatInquiryProduct(true));
  }

  /**
   * 验证询价商品标识为假时导出“否”。
   */
  @Test
  void shouldFormatFalseInquiryProductForDetailExport() {
    Assertions.assertEquals("否", ReceiveSheetDetailExportModel.formatInquiryProduct(false));
  }
}
