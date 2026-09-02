package com.lframework.xingyun.sc.dto.sale.out;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lframework.xingyun.sc.bo.sale.out.GetSaleOutSheetBo;
import com.lframework.xingyun.sc.bo.sale.out.QuerySaleOutSheetDetailBo;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

class SaleOutSheetInquiryProductMappingTest {

  /** 验证销售出库明细能够承载商品表中的询价标识。 */
  @Test
  void shouldMapInquiryProductFromProductForSaleOutDetail() {
    QuerySaleOutSheetDetailDto detail = new QuerySaleOutSheetDetailDto();
    detail.setInquiryProduct(true);

    assertTrue(detail.getInquiryProduct());

    detail.setInquiryProduct(false);

    assertFalse(detail.getInquiryProduct());
  }

  /** 验证完整销售出库单明细能够承载商品表中的询价标识。 */
  @Test
  void shouldMapInquiryProductFromProductForSaleOutFullDetail() {
    SaleOutSheetFullDto.SheetDetailDto detail = new SaleOutSheetFullDto.SheetDetailDto();
    detail.setInquiryProduct(true);

    assertTrue(detail.getInquiryProduct());

    detail.setInquiryProduct(false);

    assertFalse(detail.getInquiryProduct());
  }

  /** 验证销售出库 API 完整单据明细对外暴露询价标识。 */
  @Test
  void shouldExposeInquiryProductInSaleOutFullDetailResponse() throws NoSuchMethodException {
    assertTrue(GetSaleOutSheetBo.OrderDetailBo.class
        .getMethod("getInquiryProduct").getReturnType().equals(Boolean.class));
  }

  /** 验证销售出库查询明细 BO 对外暴露 DTO 的询价标识。 */
  @Test
  void shouldExposeInquiryProductInSaleOutQueryDetailResponse() {
    QuerySaleOutSheetDetailDto inquiryDetail = new QuerySaleOutSheetDetailDto();
    inquiryDetail.setInquiryProduct(true);

    assertTrue(new QuerySaleOutSheetDetailBo(inquiryDetail).getInquiryProduct());

    QuerySaleOutSheetDetailDto normalDetail = new QuerySaleOutSheetDetailDto();
    normalDetail.setInquiryProduct(false);

    assertFalse(new QuerySaleOutSheetDetailBo(normalDetail).getInquiryProduct());
  }

  /** 验证销售出库详情从单据报价单及商品读取询价标识。 */
  @Test
  void shouldSelectInquiryProductFromSheetQuoteDetailInSaleOutFullDetail() throws IOException {
    String mapperXml = readMapperXml();

    assertTrue(mapperXml.contains("LEFT JOIN tbl_quote_sheet AS q ON q.start_date &lt;= s.order_date"));
    assertTrue(mapperXml.contains("AND q.end_date >= s.order_date"));
    assertTrue(mapperXml.contains("LEFT JOIN tbl_quote_sheet_detail AS qd ON qd.quote_sheet_id = q.id"));
    assertTrue(mapperXml.contains("AND qd.product_id = d.product_id"));
    assertTrue(mapperXml.contains("qd.inquiry_product AS detail_inquiry_product"));
    assertTrue(mapperXml.contains("<result column=\"detail_inquiry_product\" property=\"inquiryProduct\"/>"));
  }

  /** 验证明细查询按单据日期读取生效报价单中的询价标识。 */
  @Test
  void shouldSelectInquiryProductFromQuoteDetailInSaleOutQueryDetail() throws IOException {
    String detailSql = extractSqlBlock(readMapperXml(), "SaleOutSheetDetailDto_sql");

    assertTrue(detailSql.contains("qd.inquiry_product AS inquiry_product"));
    assertTrue(detailSql.contains("LEFT JOIN tbl_quote_sheet AS q ON q.start_date &lt;= s.order_date"));
    assertTrue(detailSql.contains("AND q.end_date >= s.order_date"));
    assertTrue(detailSql.contains("LEFT JOIN tbl_quote_sheet_detail AS qd ON qd.quote_sheet_id = q.id"));
    assertTrue(detailSql.contains("AND qd.product_id = d.product_id"));
    assertFalse(detailSql.contains("NULL AS inquiry_product"));
  }

  /** 读取测试类路径中的销售出库 Mapper XML。 */
  private String readMapperXml() throws IOException {
    try (InputStream input = getClass().getResourceAsStream(
        "/mappers/sale/SaleOutSheetMapper.xml");
        Scanner scanner = new Scanner(input, StandardCharsets.UTF_8.name())) {
      return scanner.useDelimiter("\\A").next();
    }
  }

  /** 提取指定 SQL 片段，避免其他查询片段干扰断言。 */
  private String extractSqlBlock(String mapperXml, String sqlId) {
    int startIndex = mapperXml.indexOf("<sql id=\"" + sqlId + "\">");
    int endIndex = mapperXml.indexOf("</sql>", startIndex);
    return mapperXml.substring(startIndex, endIndex);
  }
}
