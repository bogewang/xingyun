package com.lframework.xingyun.sc.dto.purchase.receive;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lframework.xingyun.sc.bo.purchase.receive.GetReceiveSheetBo;
import com.lframework.xingyun.sc.bo.purchase.receive.QueryReceiveSheetDetailBo;
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

  /** 验证收货 SQL 从商品表选择询价标识并映射完整单据明细。 */
  @Test
  void shouldSelectInquiryProductInReceiveMapperQueries() throws IOException {
    String mapperXml = readMapperXml();

    assertTrue(mapperXml.contains("g.inquiry_product AS inquiry_product"));
    assertTrue(mapperXml.contains("p.inquiry_product AS detail_inquiry_product"));
    assertTrue(mapperXml.contains("<result column=\"detail_inquiry_product\" property=\"inquiryProduct\"/>"));
  }

  /** 读取测试类路径中的收货 Mapper XML。 */
  private String readMapperXml() throws IOException {
    try (InputStream input = getClass().getResourceAsStream(
        "/mappers/purchase/ReceiveSheetMapper.xml");
        Scanner scanner = new Scanner(input, StandardCharsets.UTF_8.name())) {
      return scanner.useDelimiter("\\A").next();
    }
  }
}
