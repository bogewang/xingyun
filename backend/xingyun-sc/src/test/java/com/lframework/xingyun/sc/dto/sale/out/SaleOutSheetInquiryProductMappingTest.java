package com.lframework.xingyun.sc.dto.sale.out;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lframework.xingyun.sc.bo.sale.out.GetSaleOutSheetBo;
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

  /** 验证销售出库 SQL 从商品表选择询价标识并映射完整单据明细。 */
  @Test
  void shouldSelectInquiryProductInSaleOutMapperQueries() throws IOException {
    String mapperXml = readMapperXml();

    assertTrue(mapperXml.contains("g.inquiry_product AS inquiry_product"));
    assertTrue(mapperXml.contains("g.inquiry_product AS detail_inquiry_product"));
    assertTrue(mapperXml.contains("<result column=\"detail_inquiry_product\" property=\"inquiryProduct\"/>"));
  }

  /** 读取测试类路径中的销售出库 Mapper XML。 */
  private String readMapperXml() throws IOException {
    try (InputStream input = getClass().getResourceAsStream(
        "/mappers/sale/SaleOutSheetMapper.xml");
        Scanner scanner = new Scanner(input, StandardCharsets.UTF_8.name())) {
      return scanner.useDelimiter("\\A").next();
    }
  }
}
