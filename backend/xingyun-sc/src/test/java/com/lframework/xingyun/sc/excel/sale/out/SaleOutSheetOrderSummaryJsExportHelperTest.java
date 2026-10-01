package com.lframework.xingyun.sc.excel.sale.out;

import com.lframework.xingyun.sc.dto.sale.out.QuerySaleOutSheetDetailDto;
import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证模板布局、单据分组和金额精度。 */
class SaleOutSheetOrderSummaryJsExportHelperTest {

  /** 验证长名称及手动换行自动撑高，保存后的行高和自动换行样式仍然保留。 */
  @Test
  void shouldIncreaseRowHeightForWrappedProductNames() throws Exception {
    QuerySaleOutSheetDetailDto shortName = detail("1", "3.12");
    shortName.setProductName("西红柿");
    QuerySaleOutSheetDetailDto longName = detail("1", "3.12");
    longName.setProductName("农夫水溶（多种口味混合装）");
    QuerySaleOutSheetDetailDto manualBreak = detail("1", "3.12");
    manualBreak.setProductName("苹果\r\n香蕉\n橙子");
    QuerySaleOutSheetDetailDto englishName = detail("1", "3.12");
    englishName.setProductName("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
    QuerySaleOutSheetDetailDto emptyName = detail("1", "3.12");
    emptyName.setProductName(null);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (XSSFWorkbook workbook = SaleOutSheetOrderSummaryJsExportHelper.buildWorkbook(
        Arrays.asList(shortName, longName, manualBreak, englishName, emptyName))) {
      workbook.write(output);
    }
    try (XSSFWorkbook workbook = new XSSFWorkbook(
        new ByteArrayInputStream(output.toByteArray()))) {
      Sheet sheet = workbook.getSheetAt(0);
      assertEquals(25f, sheet.getRow(4).getHeightInPoints());
      assertTrue(sheet.getRow(5).getHeightInPoints() >= 60);
      assertTrue(sheet.getRow(6).getHeightInPoints() >= 60);
      assertTrue(sheet.getRow(7).getHeightInPoints() > 25);
      assertEquals(25f, sheet.getRow(8).getHeightInPoints());
      assertTrue(sheet.getRow(5).getCell(1).getCellStyle().getWrapText());
      assertEquals(longName.getProductName(), sheet.getRow(5).getCell(1).getStringCellValue());
    }
  }

  /** 验证整数不带末尾小数点，小数数量和两位金额正常显示。 */
  @Test
  void shouldFormatIntegersWithoutTrailingDecimalPoint() throws Exception {
    QuerySaleOutSheetDetailDto integer = detail("1", "3.00");
    integer.setOrderNum(new BigDecimal("10.0000"));
    try (XSSFWorkbook workbook = SaleOutSheetOrderSummaryJsExportHelper.buildWorkbook(
        Arrays.asList(integer, detail("1", "4.56")))) {
      Sheet sheet = workbook.getSheetAt(0);
      DataFormatter formatter = new DataFormatter();
      assertEquals("1", formatter.formatCellValue(sheet.getRow(4).getCell(0)));
      assertEquals("10", formatter.formatCellValue(sheet.getRow(4).getCell(3)));
      assertEquals("10", formatter.formatCellValue(sheet.getRow(4).getCell(4)));
      assertEquals("1.5", formatter.formatCellValue(sheet.getRow(5).getCell(3)));
      assertEquals("3.00", formatter.formatCellValue(sheet.getRow(4).getCell(6)));
    }
  }

  /** 验证写入并重新读取文件后，标题、信息、合计及签字合并区域的边框完整。 */
  @Test
  void shouldPreserveMergedRegionBordersAfterSaving() throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (XSSFWorkbook workbook = SaleOutSheetOrderSummaryJsExportHelper.buildWorkbook(
        Arrays.asList(detail("1", "3.12"), detail("2", "4.56")))) {
      workbook.write(output);
    }
    try (XSSFWorkbook workbook = new XSSFWorkbook(
        new ByteArrayInputStream(output.toByteArray()))) {
      Sheet sheet = workbook.getSheetAt(0);
      for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
        CellRangeAddress region = sheet.getMergedRegion(i);
        // 空白分隔行不设置边框，只验证合并范围与行高。
        if (sheet.getRow(region.getFirstRow()).getHeightInPoints() == 50) {
          assertEquals(0, region.getFirstColumn());
          assertEquals(8, region.getLastColumn());
          continue;
        }
        for (int column = region.getFirstColumn(); column <= region.getLastColumn(); column++) {
          assertEquals(BorderStyle.THIN,
              sheet.getRow(region.getFirstRow()).getCell(column).getCellStyle().getBorderTopEnum());
          assertEquals(BorderStyle.THIN,
              sheet.getRow(region.getLastRow()).getCell(column).getCellStyle().getBorderBottomEnum());
        }
        assertEquals(BorderStyle.THIN, sheet.getRow(region.getFirstRow())
            .getCell(region.getFirstColumn()).getCellStyle().getBorderLeftEnum());
        assertEquals(BorderStyle.THIN, sheet.getRow(region.getLastRow())
            .getCell(region.getLastColumn()).getCellStyle().getBorderRightEnum());
      }
    }
  }

  /** 验证同一客户的多张单据仍然分开排列，每张单据页码固定。 */
  @Test
  void shouldKeepOrdersSeparateAndWriteNumericAmounts() throws Exception {
    try (XSSFWorkbook workbook = SaleOutSheetOrderSummaryJsExportHelper.buildWorkbook(
        Arrays.asList(detail("1", "3.12"), detail("1", "4.56"), detail("2", "8.90")))) {
      assertEquals(1, workbook.getNumberOfSheets());
      Sheet sheet = workbook.getSheetAt(0);
      assertEquals("生活服务中心采购结算单", sheet.getRow(0).getCell(0).getStringCellValue());
      assertEquals("客户名称：60-沙噶地", sheet.getRow(2).getCell(0).getStringCellValue());
      assertEquals(CellType.NUMERIC, sheet.getRow(4).getCell(6).getCellTypeEnum());
      assertEquals(3.12, sheet.getRow(4).getCell(6).getNumericCellValue(), 0.0001);
      assertEquals("第1 / 共1页", sheet.getRow(6).getCell(0).getStringCellValue());
      assertEquals("下单金额合计7.68", sheet.getRow(6).getCell(5).getStringCellValue());
      assertEquals("生活服务中心采购结算单", sheet.getRow(10).getCell(0).getStringCellValue());
      assertEquals(50f, sheet.getRow(9).getHeightInPoints());
      assertTrue(sheet.getMergedRegions().stream()
          .anyMatch(region -> "A10:I10".equals(region.formatAsString())));
      assertEquals("第1 / 共1页", sheet.getRow(15).getCell(0).getStringCellValue());
      assertEquals("下单金额合计8.90", sheet.getRow(15).getCell(5).getStringCellValue());
      assertEquals("", sheet.getRow(4).getCell(7).getStringCellValue());
      for (int row : new int[] {3, 4, 5}) {
        for (int column = 0; column < 9; column++) {
          assertEquals(HorizontalAlignment.CENTER,
              sheet.getRow(row).getCell(column).getCellStyle().getAlignmentEnum());
        }
      }
      assertEquals(HorizontalAlignment.CENTER,
          sheet.getRow(6).getCell(0).getCellStyle().getAlignmentEnum());
      assertEquals(HorizontalAlignment.CENTER,
          sheet.getRow(6).getCell(5).getCellStyle().getAlignmentEnum());
    }
  }

  /** 构造出库明细，使用小数数量覆盖数值单元格。 */
  private QuerySaleOutSheetDetailDto detail(String id, String amount) {
    QuerySaleOutSheetDetailDto detail = new QuerySaleOutSheetDetailDto();
    detail.setId(id);
    detail.setCustomerName("60-沙噶地");
    detail.setOrderDate("2026-08-08");
    detail.setProductName("后腿肉");
    detail.setUnit("斤");
    detail.setOrderNum(new BigDecimal("1.5"));
    detail.setTaxPrice(new BigDecimal("11.14"));
    detail.setTaxAmount(new BigDecimal(amount));
    return detail;
  }
}
