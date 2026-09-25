package com.lframework.xingyun.basedata.service.quote;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.support.StaticApplicationContext;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheet;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheetDetail;
import com.lframework.xingyun.basedata.mappers.ProductMapper;
import com.lframework.xingyun.basedata.mappers.quote.QuoteSheetMapper;
import com.lframework.xingyun.basedata.mappers.quote.QuoteSheetDetailMapper;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 商品多报价单追加规则测试。 */
class ProductQuoteServiceTest {
    /** 多选去重、保留既有报价并追加到末尾。 */
    @Test
    void addsMissingSheetsOnly() {
        QuoteSheetMapper sheets = mock(QuoteSheetMapper.class);
        QuoteSheetDetailMapper details = mock(QuoteSheetDetailMapper.class);
        ProductMapper products = mock(ProductMapper.class);
        ProductQuoteService service = service(sheets, details, products);
        QuoteSheet a = new QuoteSheet(); a.setId("a");
        QuoteSheet b = new QuoteSheet(); b.setId("b");
        when(sheets.selectAllForUpdate()).thenReturn(Arrays.asList(a, b));
        Product product = new Product(); product.setId("p");
        when(products.selectById("p")).thenReturn(product);
        QuoteSheetDetail old = new QuoteSheetDetail();
        old.setProductId("p"); old.setQuoteSheetId("a"); old.setSalePrice(BigDecimal.TEN);
        QuoteSheetDetail other = new QuoteSheetDetail();
        other.setProductId("other"); other.setQuoteSheetId("b"); other.setOrderNo(7);
        when(details.selectList(any())).thenReturn(Arrays.asList(old, other));
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("objectMapper", new ObjectMapper());
        new ApplicationUtil().setApplicationContext(context);
        try (MockedStatic<IdUtil> ids = mockStatic(IdUtil.class)) {
            ids.when(IdUtil::getId).thenReturn("new");
            service.addProduct("p", Arrays.asList("a", "b", "b"));
        }
        ArgumentCaptor<List<QuoteSheetDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(details).batchInsert(captor.capture());
        assertEquals(1, captor.getValue().size());
        QuoteSheetDetail added = captor.getValue().get(0);
        assertEquals("b", added.getQuoteSheetId());
        assertEquals(8, added.getOrderNo());
        assertEquals(BigDecimal.ZERO, added.getSalePrice());
        assertTrue(added.getInquiryProduct());
        assertEquals(BigDecimal.TEN, old.getSalePrice());
        when(details.selectList(any())).thenReturn(Arrays.asList(old, added));
        service.addProduct("p", Arrays.asList("a", "b"));
        verify(details, times(1)).batchInsert(any());
    }

    /** 任何一个报价单失效时不得写入部分明细。 */
    @Test
    void rejectsMissingSheetBeforeWriting() {
        QuoteSheetMapper sheets = mock(QuoteSheetMapper.class);
        QuoteSheetDetailMapper details = mock(QuoteSheetDetailMapper.class);
        ProductMapper products = mock(ProductMapper.class);
        when(sheets.selectAllForUpdate()).thenReturn(Collections.emptyList());
        assertThrows(DefaultClientException.class,
                () -> service(sheets, details, products).addProduct("p", Arrays.asList("missing")));
        verifyNoInteractions(details, products);
    }

    /** 构建隔离数据库依赖的服务。 */
    private ProductQuoteService service(QuoteSheetMapper sheets, QuoteSheetDetailMapper details, ProductMapper products) {
        ProductQuoteService service = new ProductQuoteService();
        ReflectionTestUtils.setField(service, "sheetMapper", sheets);
        ReflectionTestUtils.setField(service, "detailMapper", details);
        ReflectionTestUtils.setField(service, "productMapper", products);
        return service;
    }
}
