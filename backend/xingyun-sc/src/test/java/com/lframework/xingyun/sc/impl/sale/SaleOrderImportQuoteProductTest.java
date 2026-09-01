package com.lframework.xingyun.sc.impl.sale;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.xingyun.basedata.bo.quote.QuoteProductBo;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.basedata.service.quote.QuoteSheetService;
import com.lframework.xingyun.basedata.vo.quote.QueryQuoteProductVo;
import com.lframework.xingyun.sc.excel.sale.SaleOrderImportModel;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 销售订单导入询价商品校验测试。 */
class SaleOrderImportQuoteProductTest {

    /** 验证导入商品仅从订单日期对应的询价表中匹配。 */
    @Test
    void checkImportShouldOnlyMatchProductsInOrderDateQuoteSheet() throws Exception {
        SaleOrderServiceImpl service = new SaleOrderServiceImpl();
        QuoteSheetService quoteSheetService = mock(QuoteSheetService.class);
        ProductService productService = mock(ProductService.class);
        setField(service, "quoteSheetService", quoteSheetService);
        setField(service, "productService", productService);

        QuoteProductBo quoteProduct = new QuoteProductBo();
        quoteProduct.setProductId("quoted-product");
        when(quoteSheetService.getActiveQuoteProducts(any(QueryQuoteProductVo.class)))
                .thenReturn(Collections.singletonList(quoteProduct));

        Product product = new Product();
        product.setId("non-quoted-product");
        product.setName("测试商品");
        product.setUnit("件");
        when(productService.selectByProductName(any())).thenReturn(Collections.singletonList(product));

        SaleOrderImportModel importModel = new SaleOrderImportModel();
        importModel.setProductName("测试商品");
        importModel.setUnit("件");
        importModel.setTaxPrice(BigDecimal.ONE);
        importModel.setOrderNum(BigDecimal.ONE);
        LocalDate orderDate = LocalDate.of(2026, 8, 30);

        Assertions.assertThrows(DefaultClientException.class,
                () -> service.checkImport(Collections.singletonList(importModel), orderDate));

        ArgumentCaptor<QueryQuoteProductVo> captor = ArgumentCaptor.forClass(QueryQuoteProductVo.class);
        verify(quoteSheetService).getActiveQuoteProducts(captor.capture());
        Assertions.assertEquals(orderDate, captor.getValue().getOrderDate());
    }

    /** 验证未传订单日期时拒绝导入。 */
    @Test
    void checkImportShouldRejectMissingOrderDate() {
        SaleOrderServiceImpl service = new SaleOrderServiceImpl();

        DefaultClientException exception = Assertions.assertThrows(DefaultClientException.class,
                () -> service.checkImport(Collections.emptyList(), null));

        Assertions.assertEquals("请先选择订单日期！", exception.getMessage());
    }

    /** 为测试对象注入依赖。 */
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
