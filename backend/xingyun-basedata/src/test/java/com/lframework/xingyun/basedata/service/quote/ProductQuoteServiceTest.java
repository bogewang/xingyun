package com.lframework.xingyun.basedata.service.quote;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.support.StaticApplicationContext;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.bo.quote.QuoteProductBo;
import com.lframework.xingyun.basedata.converter.quote.QuoteSheetConverterImpl;
import com.lframework.xingyun.basedata.bo.product.info.QueryProductBo;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheet;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheetDetail;
import com.lframework.xingyun.basedata.mappers.ProductMapper;
import com.lframework.xingyun.basedata.mappers.quote.QuoteSheetMapper;
import com.lframework.xingyun.basedata.mappers.quote.QuoteSheetDetailMapper;
import java.util.*;
import java.math.BigDecimal;
import com.lframework.xingyun.basedata.vo.product.info.SaveProductQuoteVo;
import com.lframework.xingyun.basedata.vo.product.info.SaveProductQuoteVo.QuoteRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 商品多报价单追加规则测试。 */
class ProductQuoteServiceTest {
    /** 为隔离数据库的测试初始化实体字段元数据。 */
    @BeforeAll
    static void initializeTableMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "quote-test");
        assistant.setCurrentNamespace("quote-test");
        TableInfoHelper.initTableInfo(assistant, QuoteSheetDetail.class);
        TableInfoHelper.initTableInfo(assistant, QuoteSheet.class);
    }

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

    /** 多报价单按查询顺序展示，重复明细去重，无报价商品显示空值。 */
    @Test
    void fillsAllQuoteNamesInBatch() {
        QuoteSheetMapper sheets = mock(QuoteSheetMapper.class);
        QuoteSheetDetailMapper details = mock(QuoteSheetDetailMapper.class);
        ProductMapper products = mock(ProductMapper.class);
        QueryProductBo first = new QueryProductBo(); first.setId("p");
        QueryProductBo second = new QueryProductBo(); second.setId("empty");
        QuoteSheet a = new QuoteSheet(); a.setId("a"); a.setName("九月报价");
        QuoteSheet b = new QuoteSheet(); b.setId("b"); b.setName("八月报价");
        QuoteSheetDetail da = new QuoteSheetDetail(); da.setProductId("p"); da.setQuoteSheetId("a");
        QuoteSheetDetail db = new QuoteSheetDetail(); db.setProductId("p"); db.setQuoteSheetId("b");
        when(details.selectList(any())).thenReturn(Arrays.asList(db, da, da));
        when(sheets.selectList(any())).thenReturn(Arrays.asList(a, b));
        service(sheets, details, products).fillQuoteSheetNames(Arrays.asList(first, second));
        assertEquals("九月报价、八月报价", first.getQuoteSheetNames());
        assertEquals(Arrays.asList("a", "b"), first.getQuoteSheetIds());
        assertTrue(second.getQuoteSheetIds().isEmpty());
        assertEquals("", second.getQuoteSheetNames());
        verify(details, times(1)).selectList(any());
        verify(sheets, times(1)).selectList(any());
        verifyNoInteractions(products);
    }

    /** 空页面不查询数据库，无关联时不继续查询报价单。 */
    @Test
    void skipsUnnecessaryQuoteQueries() {
        QuoteSheetMapper sheets = mock(QuoteSheetMapper.class);
        QuoteSheetDetailMapper details = mock(QuoteSheetDetailMapper.class);
        ProductMapper products = mock(ProductMapper.class);
        ProductQuoteService service = service(sheets, details, products);
        service.fillQuoteSheetNames(Collections.emptyList());
        verifyNoInteractions(sheets, details, products);
        QueryProductBo product = new QueryProductBo(); product.setId("p");
        when(details.selectList(any())).thenReturn(Collections.emptyList());
        service.fillQuoteSheetNames(Collections.singletonList(product));
        assertEquals("", product.getQuoteSheetNames());
        assertTrue(product.getQuoteSheetIds().isEmpty());
        verifyNoInteractions(sheets, products);
    }

    /** 回显各报价单各自的真实价格及询价标识。 */
    @Test
    void returnsExistingPricesAndInquiryFlags() {
        QuoteSheetMapper sheets = mock(QuoteSheetMapper.class);
        QuoteSheetDetailMapper details = mock(QuoteSheetDetailMapper.class);
        ProductQuoteService service = service(sheets, details, mock(ProductMapper.class));
        ReflectionTestUtils.setField(service, "converter", new QuoteSheetConverterImpl());
        QuoteSheetDetail a = new QuoteSheetDetail();
        a.setQuoteSheetId("a"); a.setProductId("p");
        a.setSalePrice(new BigDecimal("12.350000")); a.setInquiryProduct(false);
        QuoteSheetDetail b = new QuoteSheetDetail();
        b.setQuoteSheetId("b"); b.setProductId("p");
        b.setSalePrice(BigDecimal.ZERO); b.setInquiryProduct(true);
        when(details.selectList(any())).thenReturn(Arrays.asList(a, b));
        List<QuoteProductBo> result = service.productDetails("p");
        assertEquals("a", result.get(0).getQuoteSheetId());
        assertEquals(a.getSalePrice(), result.get(0).getSalePrice());
        assertFalse(result.get(0).getInquiryProduct());
        assertEquals("b", result.get(1).getQuoteSheetId());
        assertEquals(BigDecimal.ZERO, result.get(1).getSalePrice());
        assertTrue(result.get(1).getInquiryProduct());
    }

    /** 编辑已有报价应保留明细标识、快照和排序，并保存零价及否。 */
    @Test
    void updatesExistingPriceAndInquiry() {
        QuoteSheetMapper sheets = mock(QuoteSheetMapper.class);
        QuoteSheetDetailMapper details = mock(QuoteSheetDetailMapper.class);
        ProductMapper products = mock(ProductMapper.class);
        QuoteSheet sheet = new QuoteSheet(); sheet.setId("a");
        when(sheets.selectAllForUpdate()).thenReturn(Collections.singletonList(sheet));
        Product product = new Product(); product.setId("p");
        when(products.selectById("p")).thenReturn(product);
        QuoteSheetDetail old = new QuoteSheetDetail();
        old.setId("detail"); old.setProductId("p"); old.setQuoteSheetId("a");
        old.setOrderNo(5); old.setProductSnapshot("original");
        old.setSalePrice(BigDecimal.TEN); old.setInquiryProduct(true);
        when(details.selectList(any())).thenReturn(Collections.singletonList(old));
        SaveProductQuoteVo vo = new SaveProductQuoteVo(); vo.setProductId("p");
        QuoteRow row = new QuoteRow(); row.setQuoteSheetId("a");
        row.setSalePrice(BigDecimal.ZERO); row.setInquiryProduct(false);
        vo.setQuotes(Collections.singletonList(row));
        service(sheets, details, products).saveProductQuotes(vo);
        ArgumentCaptor<List<QuoteSheetDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(details).batchInsert(captor.capture());
        QuoteSheetDetail saved = captor.getValue().get(0);
        assertEquals("detail", saved.getId());
        assertEquals("original", saved.getProductSnapshot());
        assertEquals(5, saved.getOrderNo());
        assertEquals(BigDecimal.ZERO, saved.getSalePrice());
        assertFalse(saved.getInquiryProduct());
    }

    /** 非法价格必须在数据库写入前拒绝。 */
    @Test
    void rejectsInvalidEditedPrice() {
        QuoteSheetMapper sheets = mock(QuoteSheetMapper.class);
        QuoteSheetDetailMapper details = mock(QuoteSheetDetailMapper.class);
        ProductMapper products = mock(ProductMapper.class);
        SaveProductQuoteVo vo = new SaveProductQuoteVo(); vo.setProductId("p");
        QuoteRow row = new QuoteRow(); row.setQuoteSheetId("a"); row.setInquiryProduct(false);
        vo.setQuotes(Collections.singletonList(row));
        for (BigDecimal price : Arrays.asList(null, new BigDecimal("-1"), new BigDecimal("0.001"))) {
            row.setSalePrice(price);
            assertThrows(DefaultClientException.class, () -> service(sheets, details, products).saveProductQuotes(vo));
        }
        verifyNoInteractions(sheets, details, products);
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
