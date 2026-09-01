package com.lframework.xingyun.basedata.impl.quote;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheet;
import com.lframework.xingyun.basedata.enums.quote.QuoteSheetStatus;
import com.lframework.xingyun.basedata.vo.quote.QuoteSheetProductVo;
import com.lframework.xingyun.basedata.converter.quote.QuoteSheetConverter;
import com.lframework.xingyun.basedata.converter.quote.QuoteSheetConverterImpl;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.ProductUnit;
import com.lframework.xingyun.basedata.mappers.ProductMapper;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lframework.xingyun.basedata.mappers.quote.QuoteSheetDetailMapper;
import com.lframework.xingyun.basedata.mappers.quote.QuoteSheetMapper;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.StaticApplicationContext;
import org.mockito.Mockito;
import org.mockito.MockedStatic;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

/** 报价单服务规则测试。 */
public class QuoteSheetServiceImplTest {
  /** 相邻闭区间不重叠。 */
  @Test public void shouldTreatAdjacentDateRangesAsNonOverlapping() { Assert.assertFalse(QuoteSheetServiceImpl.isDateRangeOverlapped(date("2026-08-01"),date("2026-08-31"),date("2026-09-01"),date("2026-09-30"))); }
  /** 端点相同的闭区间应重叠。 */
  @Test public void shouldTreatSharedEndpointAsOverlapping() { Assert.assertTrue(QuoteSheetServiceImpl.isDateRangeOverlapped(date("2026-08-01"),date("2026-08-31"),date("2026-08-31"),date("2026-09-30"))); }
  /** 重叠定价周期应拒绝。 */
  @Test(expectedExceptions=DefaultClientException.class,expectedExceptionsMessageRegExp=".*定价周期.*冲突.*") public void shouldRejectOverlappedQuotePeriod() { QuoteSheetServiceImpl.assertNoDateRangeOverlap("quote-2",date("2026-08-01"),date("2026-08-31"),Collections.singletonList(sheet("quote-1","2026-08-31","2026-09-30"))); }
  /** 与停用报价单重叠时应允许保存。 */
  @Test public void shouldAllowOverlappedPeriodWithDisabledQuoteSheet() { QuoteSheet disabled=sheet("quote-1","2026-08-31","2026-09-30"); disabled.setStatus(QuoteSheetStatus.DISABLED); QuoteSheetServiceImpl.assertNoDateRangeOverlap("quote-2",date("2026-08-01"),date("2026-08-31"),Collections.singletonList(disabled)); }
  /** 排除自身后允许更新原报价周期。 */
  @Test public void shouldExcludeCurrentQuoteSheetFromOverlapCheck() { QuoteSheetServiceImpl.assertNoDateRangeOverlap("quote-1",date("2026-08-01"),date("2026-08-31"),Collections.singletonList(sheet("quote-1","2026-08-01","2026-08-31"))); }
  /** 同单重复商品应拒绝。 */
  @Test(expectedExceptions=DefaultClientException.class,expectedExceptionsMessageRegExp=".*商品不能重复.*") public void shouldRejectDuplicatedProducts() { QuoteSheetProductVo first=new QuoteSheetProductVo(); first.setProductId("product-1"); QuoteSheetProductVo second=new QuoteSheetProductVo(); second.setProductId("product-1"); QuoteSheetServiceImpl.assertNoDuplicatedProducts(Arrays.asList(first,second)); }
  /** 停用报价单不参与生效报价查询。 */
  @Test public void shouldNotTreatDisabledQuoteAsActive() { QuoteSheet disabled=sheet("quote-1","2026-08-01","2026-08-31"); disabled.setStatus(QuoteSheetStatus.DISABLED); Assert.assertFalse(QuoteSheetServiceImpl.isActiveOn(disabled,date("2026-08-15"))); }
  /** 服务层保存时空商品明细应被拒绝。 */
  @Test(expectedExceptions=DefaultClientException.class,expectedExceptionsMessageRegExp=".*商品明细不能为空.*") public void shouldRejectEmptyProductsInServiceLayer() { QuoteSheetServiceImpl.assertBasicSheetData(date("2026-08-01"),date("2026-08-31"),Collections.emptyList()); }
  /** 启用前非法日期应被共用校验拒绝。 */
  @Test(expectedExceptions=DefaultClientException.class,expectedExceptionsMessageRegExp=".*开始日期不能晚于.*") public void shouldRejectInvalidDatesBeforeEnabling() { QuoteSheetServiceImpl.assertBasicSheetData(date("2026-08-31"),date("2026-08-01"),Collections.singletonList("product-1")); }
  /** 导入的是否询价商品留空时默认是，并支持显式填写否。 */
  @Test public void shouldParseImportInquiryProductWithDefaultTrue() { Assert.assertTrue(QuoteSheetServiceImpl.parseImportInquiryProduct(null,2)); Assert.assertTrue(QuoteSheetServiceImpl.parseImportInquiryProduct("是",2)); Assert.assertFalse(QuoteSheetServiceImpl.parseImportInquiryProduct("否",2)); }
  /** 导入的是否询价商品仅允许填写是或否。 */
  @Test(expectedExceptions=DefaultClientException.class,expectedExceptionsMessageRegExp=".*是否询价商品.*只能填写.*") public void shouldRejectInvalidImportInquiryProduct() { QuoteSheetServiceImpl.parseImportInquiryProduct("未知",2); }
  /** 导入匹配到商品后，返回页面的商品名称必须以商品主数据为准。 */
  @Test public void shouldFillProductNameAfterMatchingQuoteSheetImport() throws Exception {
    QuoteSheetServiceImpl service=new QuoteSheetServiceImpl();
    com.lframework.xingyun.basedata.service.product.ProductService productService=Mockito.mock(com.lframework.xingyun.basedata.service.product.ProductService.class);
    com.lframework.xingyun.basedata.service.product.ProductUnitService productUnitService=Mockito.mock(com.lframework.xingyun.basedata.service.product.ProductUnitService.class);
    Product product=new Product(); product.setId("product-1"); product.setName("标准商品名"); product.setCode("P001");
    ProductUnit unit=new ProductUnit(); unit.setUnitName("箱");
    Mockito.when(productService.selectByProductName(Mockito.anyList())).thenReturn(Collections.singletonList(product));
    Mockito.when(productUnitService.getAvailableByProductId("product-1")).thenReturn(Collections.singletonList(unit));
    Mockito.when(productUnitService.getAvailableByUnitName("product-1","箱")).thenReturn(unit);
    setField(service,"productService",productService); setField(service,"productUnitService",productUnitService);
    com.lframework.xingyun.basedata.excel.quote.QuoteSheetImportModel row=new com.lframework.xingyun.basedata.excel.quote.QuoteSheetImportModel(); row.setName(" 标准商品名 "); row.setUnit("箱");
    Assert.assertEquals(service.checkImport(Collections.singletonList(row)).get(0).getName(),"标准商品名");
  }
  /** 有效报价 SQL 必须排除停用商品。 */
  @Test public void shouldExcludeDisabledProductsFromActiveQuoteQuery() throws Exception { String sql=new String(Files.readAllBytes(Paths.get("src/main/resources/mappers/quote/QuoteSheetMapper.xml")),StandardCharsets.UTF_8); Assert.assertTrue(sql.contains("p.available = TRUE")); }
  /** 报价单商品明细查询应提供报价单、商品及是否询价筛选字段。 */
  @Test public void shouldProvideQuoteSheetDetailQueryMapper() throws Exception { String sql=new String(Files.readAllBytes(Paths.get("src/main/resources/mappers/quote/QuoteSheetDetailMapper.xml")),StandardCharsets.UTF_8); Assert.assertTrue(sql.contains("queryDetails")); Assert.assertTrue(sql.contains("vo.quoteSheetName")); Assert.assertTrue(sql.contains("vo.productKeyword")); Assert.assertTrue(sql.contains("vo.inquiryProduct")); Assert.assertTrue(sql.contains("base_data_unit")); }
  /** 商品选择器仅接收报价单 ID，并由数据库查询该报价单的已有明细。 */
  @Test public void shouldExcludeQuoteSheetDetailsByQuoteSheetId() throws Exception { String sql=new String(Files.readAllBytes(Paths.get("src/main/resources/mappers/product/ProductMapper.xml")),StandardCharsets.UTF_8); Assert.assertTrue(sql.contains("vo.quoteSheetId")); Assert.assertTrue(sql.contains("NOT EXISTS")); Assert.assertTrue(sql.contains("tbl_quote_sheet_detail")); Assert.assertFalse(sql.contains("excludeProductIds")); }
  /** 报价单明细批量保存必须包含是否询价字段。 */
  @Test public void shouldPersistInquiryProductInQuoteSheetDetail() throws Exception { String sql=new String(Files.readAllBytes(Paths.get("src/main/resources/mappers/quote/QuoteSheetDetailMapper.xml")),StandardCharsets.UTF_8); Assert.assertTrue(sql.contains("inquiry_product")); Assert.assertTrue(sql.contains("#{item.inquiryProduct}")); }
  /** 已被引用的报价单仍允许修改，删除限制由删除流程单独处理。 */
  @Test public void shouldAllowUpdatingReferencedQuoteSheet() throws Exception { String source=new String(Files.readAllBytes(Paths.get("src/main/java/com/lframework/xingyun/basedata/impl/quote/QuoteSheetServiceImpl.java")),StandardCharsets.UTF_8); Assert.assertFalse(source.contains("报价单已被业务单据使用，不能修改！")); }
  /** 所有写流程共用的入口先锁定当前租户报价单范围。 */
  @Test public void shouldLockTenantRangeBeforeLocatingQuoteSheet() throws Exception { QuoteSheetServiceImpl service=new QuoteSheetServiceImpl(); QuoteSheetMapper mapper=Mockito.mock(QuoteSheetMapper.class); QuoteSheet sheet=sheet("quote-1","2026-08-01","2026-08-31"); Mockito.when(mapper.selectByTenantIdForUpdate(Mockito.<String>any())).thenReturn(Collections.singletonList(sheet)); setBaseMapper(service,mapper); Assert.assertEquals(QuoteSheetServiceImpl.requireLockedSheet("quote-1",service.lockTenantQuoteSheets()),sheet); Mockito.verify(mapper).selectByTenantIdForUpdate(Mockito.<String>any()); }
  /** 批量持久化必须把租户 ID 传入 Mapper 明细实体。 */
  @Test public void shouldPassTenantIdToBatchInsertMapper() throws Exception {
    StaticApplicationContext applicationContext=new StaticApplicationContext();
    applicationContext.getBeanFactory().registerSingleton("objectMapper",new ObjectMapper());
    new ApplicationUtil().setApplicationContext(applicationContext);
    QuoteSheetServiceImpl service=new QuoteSheetServiceImpl();
    QuoteSheetDetailMapper mapper=Mockito.mock(QuoteSheetDetailMapper.class);
    ProductMapper productMapper=Mockito.mock(ProductMapper.class);
    AtomicReference<List<com.lframework.xingyun.basedata.entity.quote.QuoteSheetDetail>> detailsRef=new AtomicReference<>();
    Mockito.doAnswer(invocation->{ detailsRef.set(invocation.getArgument(0)); return 1; }).when(mapper).batchInsert(Mockito.anyList());
    setField(service,"quoteSheetDetailMapper",mapper);
    setField(service,"productMapper",productMapper);
    setField(service,"quoteSheetConverter",new QuoteSheetConverterImpl());
    QuoteSheetProductVo product=new QuoteSheetProductVo(); product.setProductId("product-1"); product.setSalePrice(BigDecimal.ONE);
    Product savedProduct=new Product(); savedProduct.setId("product-1"); savedProduct.setName("测试商品");
    Mockito.when(productMapper.selectList(Mockito.any())).thenReturn(Collections.singletonList(savedProduct));
    try (MockedStatic<com.lframework.starter.web.core.utils.IdUtil> idUtil=Mockito.mockStatic(com.lframework.starter.web.core.utils.IdUtil.class)) { idUtil.when(com.lframework.starter.web.core.utils.IdUtil::getId).thenReturn("detail-1"); service.saveDetails("quote-1","tenant-1",Collections.singletonList(product)); }
    Assert.assertNotNull(detailsRef.get());
    Assert.assertEquals(detailsRef.get().get(0).getTenantId(),"tenant-1");
    Assert.assertTrue(detailsRef.get().get(0).getInquiryProduct());
    Assert.assertNotNull(detailsRef.get().get(0).getProductSnapshot());
  }
  /** 构造报价单。 */
  private QuoteSheet sheet(String id,String start,String end) { QuoteSheet sheet=new QuoteSheet(); sheet.setId(id); sheet.setStartDate(date(start)); sheet.setEndDate(date(end)); sheet.setStatus(QuoteSheetStatus.ENABLED); return sheet; }
  /** 解析测试日期。 */
  private LocalDate date(String value) { return LocalDate.parse(value); }
  /** 通过反射注入服务依赖。 */
  private void setField(Object target,String name,Object value) throws Exception { Field field=QuoteSheetServiceImpl.class.getDeclaredField(name); field.setAccessible(true); field.set(target,value); }
  /** 为服务注入基础 Mapper。 */
  private void setBaseMapper(QuoteSheetServiceImpl service,QuoteSheetMapper mapper) throws Exception { Class<?> type=service.getClass(); while(type!=null){ try { Field field=type.getDeclaredField("baseMapper"); field.setAccessible(true); field.set(service,mapper); return; } catch(NoSuchFieldException ignored) { type=type.getSuperclass(); } } throw new IllegalStateException("未找到 BaseMapper 字段"); }
}
