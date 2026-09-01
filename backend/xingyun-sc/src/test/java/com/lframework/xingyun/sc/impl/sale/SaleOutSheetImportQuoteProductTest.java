package com.lframework.xingyun.sc.impl.sale;

import com.lframework.xingyun.basedata.bo.quote.QuoteProductBo;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.ProductUnit;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.basedata.service.product.ProductUnitService;
import com.lframework.xingyun.basedata.service.quote.QuoteSheetService;
import com.lframework.xingyun.basedata.vo.quote.QueryQuoteProductVo;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetImportModel;
import com.lframework.xingyun.sc.vo.sale.out.SaleOutProductVo;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 销售出库导入询价商品校验测试。 */
class SaleOutSheetImportQuoteProductTest {

    /** 验证不在当月询价表的商品不能作为有效商品返回。 */
    @Test
    void checkImportShouldNotMatchProductOutsideOrderDateQuoteSheet() throws Exception {
        SaleOutSheetServiceImpl service = new SaleOutSheetServiceImpl();
        ProductService productService = mock(ProductService.class);
        ProductUnitService productUnitService = mock(ProductUnitService.class);
        QuoteSheetService quoteSheetService = mock(QuoteSheetService.class);
        setField(service, "productService", productService);
        setField(service, "productUnitService", productUnitService);
        setField(service, "quoteSheetService", quoteSheetService);

        QuoteProductBo quoteProduct = new QuoteProductBo();
        quoteProduct.setProductId("another-product");
        when(quoteSheetService.getActiveQuoteProducts(any(QueryQuoteProductVo.class)))
                .thenReturn(Collections.singletonList(quoteProduct));

        Product product = new Product();
        product.setId("squid-product");
        product.setName("鱿鱼串");
        product.setCode("P26082900166");
        when(productService.selectByProductName(anyList())).thenReturn(Collections.singletonList(product));

        ProductUnit unit = new ProductUnit();
        unit.setId("unit-1");
        unit.setUnitName("串");
        unit.setConversionRate(BigDecimal.ONE);
        when(productUnitService.getAvailableByProductId("squid-product"))
                .thenReturn(Collections.singletonList(unit));
        when(productUnitService.getAvailableByUnitName("squid-product", "串")).thenReturn(unit);

        SaleOutSheetImportModel importModel = new SaleOutSheetImportModel();
        importModel.setProductName("鱿鱼串");
        importModel.setUnit("串");
        importModel.setTaxPrice(new BigDecimal("3.15"));
        importModel.setOrderNum(BigDecimal.ONE);

        LocalDate orderDate = LocalDate.of(2026, 8, 30);
        SaleOutProductVo result = service.checkImport(Collections.singletonList(importModel), orderDate).get(0);

        Assertions.assertNull(result.getProductId(), "不在订单日期询价表中的商品不应作为有效商品返回");
        ArgumentCaptor<QueryQuoteProductVo> captor = ArgumentCaptor.forClass(QueryQuoteProductVo.class);
        verify(quoteSheetService).getActiveQuoteProducts(captor.capture());
        Assertions.assertEquals(orderDate, captor.getValue().getOrderDate());
    }

    /** 为测试对象注入依赖。 */
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
