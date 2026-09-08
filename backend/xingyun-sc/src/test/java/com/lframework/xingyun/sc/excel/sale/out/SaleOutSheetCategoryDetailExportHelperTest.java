package com.lframework.xingyun.sc.excel.sale.out;

import com.lframework.xingyun.sc.dto.sale.out.QuerySaleOutSheetDetailDto;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * 销售出库商品备注二汇总导出测试。
 */
public class SaleOutSheetCategoryDetailExportHelperTest {

  /**
   * 验证按客户拆分工作表，并按日期和商品备注二汇总金额。
   *
   * @throws Exception 导出或读取工作簿失败时抛出
   */
  @Test
  public void shouldExportCategoryDetailByCustomerAndDay() throws Exception {
    QuerySaleOutSheetDetailDto frozenDetail = createDetail("300分队", "2026-08-11", "备注二A",
        new BigDecimal("100"), new BigDecimal("80"));
    QuerySaleOutSheetDetailDto condimentDetail = createDetail("300分队", "2026-08-11", "备注二B",
        new BigDecimal("20"), null);
    QuerySaleOutSheetDetailDto otherCustomerDetail = createDetail("400分队", "2026-08-12", "备注二A",
        new BigDecimal("30"), new BigDecimal("0"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    SaleOutSheetCategoryDetailExportHelper.export(
        Arrays.asList(frozenDetail, condimentDetail, otherCustomerDetail),
        LocalDate.of(2026, 8, 11), LocalDate.of(2026, 8, 12), response);

    try (XSSFWorkbook workbook = new XSSFWorkbook(
        new ByteArrayInputStream(response.getContentAsByteArray()))) {
      Assert.assertEquals(2, workbook.getNumberOfSheets());
      Assert.assertNotNull(workbook.getSheet("300分队"));
      Assert.assertEquals("300分队8月份汇总表",
          workbook.getSheet("300分队").getRow(0).getCell(0).getStringCellValue());
      Assert.assertEquals("备注二A", workbook.getSheet("300分队").getRow(1).getCell(1)
          .getStringCellValue());
      Assert.assertEquals("备注二B", workbook.getSheet("300分队").getRow(1).getCell(2)
          .getStringCellValue());
      Assert.assertEquals(80D, workbook.getSheet("300分队").getRow(2).getCell(1)
          .getNumericCellValue(), 0.001D);
      Assert.assertEquals(20D, workbook.getSheet("300分队").getRow(2).getCell(2)
          .getNumericCellValue(), 0.001D);
      Assert.assertEquals(100D, workbook.getSheet("300分队").getRow(2).getCell(3)
          .getNumericCellValue(), 0.001D);
      Assert.assertEquals("合计（元）", workbook.getSheet("300分队").getRow(4).getCell(0)
          .getStringCellValue());
      Assert.assertEquals("壹佰元整", workbook.getSheet("300分队").getRow(5).getCell(1)
          .getStringCellValue());
      Assert.assertEquals("生活服务中心：", workbook.getSheet("300分队").getRow(6).getCell(0)
          .getStringCellValue());
    }
  }

  /**
   * 创建用于导出的销售出库明细。
   *
   * @param customerName 客户名称
   * @param orderDate 订单日期
   * @param productRemark2 商品备注二
   * @param taxAmount 单据金额
   * @param confirmAmt 验收金额
   * @return 销售出库明细
   */
  private QuerySaleOutSheetDetailDto createDetail(String customerName, String orderDate,
      String productRemark2, BigDecimal taxAmount, BigDecimal confirmAmt) {
    QuerySaleOutSheetDetailDto detail = new QuerySaleOutSheetDetailDto();
    detail.setCustomerName(customerName);
    detail.setOrderDate(orderDate);
    detail.setProductRemark2(productRemark2);
    detail.setTaxAmount(taxAmount);
    detail.setConfirmAmt(confirmAmt);
    return detail;
  }
}
