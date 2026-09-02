package com.lframework.xingyun.sc.dto.purchase.receive;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lframework.xingyun.sc.bo.purchase.receive.GetReceiveSheetBo;
import com.lframework.xingyun.sc.bo.purchase.receive.QueryReceiveSheetDetailBo;
import com.lframework.xingyun.sc.vo.purchase.receive.QueryReceiveSheetVo;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

class ReceiveSheetInquiryProductMappingTest {

  /** 验证收货明细能够承载商品表中的询价标识。 */
  @Test
  void shouldMapInquiryProductFromProductForReceiveDetail() {
    QueryReceiveSheetDetailDto detail = new QueryReceiveSheetDetailDto();
    detail.setInquiryProduct(true);

    assertTrue(detail.getInquiryProduct());

    detail.setInquiryProduct(false);

    assertFalse(detail.getInquiryProduct());
  }

  /** 验证完整收货单明细能够承载商品表中的询价标识。 */
  @Test
  void shouldMapInquiryProductFromProductForReceiveFullDetail() {
    ReceiveSheetFullDto.OrderDetailDto detail = new ReceiveSheetFullDto.OrderDetailDto();
    detail.setInquiryProduct(true);

    assertTrue(detail.getInquiryProduct());

    detail.setInquiryProduct(false);

    assertFalse(detail.getInquiryProduct());
  }

  /** 验证收货 API 完整单据明细对外暴露询价标识。 */
  @Test
  void shouldExposeInquiryProductInReceiveFullDetailResponse() throws NoSuchMethodException {
    assertTrue(GetReceiveSheetBo.OrderDetailBo.class
        .getMethod("getInquiryProduct").getReturnType().equals(Boolean.class));
  }

  /** 验证收货查询明细 BO 对外暴露 DTO 的询价标识。 */
  @Test
  void shouldExposeInquiryProductInReceiveQueryDetailResponse() {
    QueryReceiveSheetDetailDto inquiryDetail = new QueryReceiveSheetDetailDto();
    inquiryDetail.setInquiryProduct(true);

    assertTrue(new QueryReceiveSheetDetailBo(inquiryDetail).getInquiryProduct());

    QueryReceiveSheetDetailDto normalDetail = new QueryReceiveSheetDetailDto();
    normalDetail.setInquiryProduct(false);

    assertFalse(new QueryReceiveSheetDetailBo(normalDetail).getInquiryProduct());
  }

  /** 验证采购入库明细查询按单据日期读取生效报价单中的询价标识。 */
  @Test
  void shouldSelectInquiryProductInReceiveMapperQueries() throws IOException {
    String detailSql = extractSqlBlock(readMapperXml(), "ReceiveSheetDetailDto_sql");

    assertTrue(detailSql.contains("qd.inquiry_product AS inquiry_product"));
    assertTrue(detailSql.contains("LEFT JOIN tbl_quote_sheet AS q ON q.start_date &lt;= r.order_date"));
    assertTrue(detailSql.contains("AND q.end_date >= r.order_date"));
    assertTrue(detailSql.contains("LEFT JOIN tbl_quote_sheet_detail AS qd ON qd.quote_sheet_id = q.id"));
    assertTrue(detailSql.contains("AND qd.product_id = d.product_id"));
    assertFalse(detailSql.contains("NULL AS inquiry_product"));
  }

  /** 验证采购入库明细查询支持按是否询价商品筛选。 */
  @Test
  void shouldFilterReceiveQueryDetailByInquiryProduct() throws IOException, NoSuchMethodException {
    String queryDetailSql = extractSelectBlock(readMapperXml(), "queryDetail");

    assertTrue(QueryReceiveSheetVo.class.getMethod("getInquiryProduct").getReturnType()
        .equals(Boolean.class));
    assertTrue(queryDetailSql.contains("vo.inquiryProduct != null"));
    assertTrue(queryDetailSql.contains("qd.inquiry_product = #{vo.inquiryProduct}"));
  }

  /** 读取测试类路径中的收货 Mapper XML。 */
  private String readMapperXml() throws IOException {
    try (InputStream input = getClass().getResourceAsStream(
        "/mappers/purchase/ReceiveSheetMapper.xml");
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

  /** 提取指定查询语句，避免其他查询语句干扰断言。 */
  private String extractSelectBlock(String mapperXml, String selectId) {
    int startIndex = mapperXml.indexOf("<select id=\"" + selectId + "\"");
    int endIndex = mapperXml.indexOf("</select>", startIndex);
    return mapperXml.substring(startIndex, endIndex);
  }
}
