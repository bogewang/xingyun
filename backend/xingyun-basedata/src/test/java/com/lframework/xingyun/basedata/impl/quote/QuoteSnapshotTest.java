package com.lframework.xingyun.basedata.impl.quote;

import com.lframework.xingyun.basedata.bo.quote.QuoteProductBo;
import com.lframework.xingyun.basedata.converter.quote.QuoteSheetConverter;
import com.lframework.xingyun.basedata.converter.quote.QuoteSheetConverterImpl;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheetDetail;
import com.lframework.xingyun.basedata.vo.quote.QuoteSheetProductVo;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** 报价快照转换回归测试。 */
class QuoteSnapshotTest {
  /** 同商品不同报价允许独立名称、空规格和单位，不丢失换算率。 */
  @Test
  void shouldKeepIndependentQuoteSnapshots() {
    QuoteSheetConverter converter = new QuoteSheetConverterImpl();
    QuoteSheetProductVo vo = new QuoteSheetProductVo();
    vo.setProductId("product-1");
    vo.setProductName("项目A名称");
    vo.setSpec("100g");
    vo.setUnitName("千克");
    vo.setConversionRate(BigDecimal.ONE);
    QuoteSheetDetail first = converter.toDetail(vo, "quote-a");
    vo.setProductName("项目B名称");
    vo.setSpec("");
    vo.setUnitName("公斤");
    QuoteProductBo second = converter.toProductBo(converter.toDetail(vo, "quote-b"));
    Assertions.assertEquals("项目A名称", first.getProductName());
    Assertions.assertEquals("100g", first.getSpec());
    Assertions.assertEquals("项目B名称", second.getName());
    Assertions.assertEquals("", second.getSpec());
    Assertions.assertEquals("公斤", second.getUnit());
    Assertions.assertEquals(BigDecimal.ONE, second.getConversionRate());
    Assertions.assertEquals(first.getProductId(), second.getProductId());
  }
}
