package com.lframework.xingyun.sc.excel.sale.out;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 销售出库明细导出模型测试。
 */
class SaleOutSheetDetailExportModelTest {

  /**
   * 验证商品备注二映射到销售出库明细导出列。
  */
  @Test
  void shouldMapProductRemark2ForDetailExport() throws IOException {
    String modelSource = readSource(
        "src/main/java/com/lframework/xingyun/sc/excel/sale/out/"
            + "SaleOutSheetDetailExportModel.java");

    Assertions.assertTrue(modelSource.contains("@ExcelProperty(\"备注二\")"));
    Assertions.assertTrue(
        modelSource.contains("setProductRemark2(dto.getProductRemark2())"));
  }

  /**
   * 验证询价商品标识为空时导出“否”。
   */
  @Test
  void shouldFormatNullInquiryProductForDetailExport() {
    Assertions.assertEquals("否", SaleOutSheetDetailExportModel.formatInquiryProduct(null));
  }

  /**
   * 验证销售明细导出链路只映射一次询价商品标识。
   */
  @Test
  void shouldAssignInquiryProductOnlyOnceInExportFlow() throws IOException {
    String modelSource = readSource(
        "src/main/java/com/lframework/xingyun/sc/excel/sale/out/"
            + "SaleOutSheetDetailExportModel.java");
    String workerSource = readSource(
        "src/main/java/com/lframework/xingyun/sc/excel/sale/out/"
            + "SaleOutSheetDetailExportTaskWorker.java");

    Assertions.assertEquals(1,
        countOccurrences(modelSource + workerSource, "setInquiryProduct("));
  }

  /**
   * 读取模块内生产源码。
   *
   * @param relativePath 相对模块目录的源码路径
   * @return 源码内容
   * @throws IOException 读取失败
   */
  private String readSource(String relativePath) throws IOException {
    Path path = Paths.get(relativePath);
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }

  /**
   * 统计文本出现次数。
   *
   * @param source 源文本
   * @param fragment 目标片段
   * @return 出现次数
   */
  private int countOccurrences(String source, String fragment) {
    return (source.length() - source.replace(fragment, "").length()) / fragment.length();
  }
}
