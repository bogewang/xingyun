package com.lframework.xingyun.sc.impl.sale;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.xingyun.basedata.bo.quote.QuoteProductBo;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import com.lframework.xingyun.sc.enums.SaleOutSheetStatus;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.mappers.SaleOutSheetMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.lframework.xingyun.sc.dto.sale.out.QuerySaleOutSheetDetailDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetFullDto;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetInvoiceDetailExportModel;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetImportModel;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetQueryImportModel;
import com.lframework.xingyun.sc.vo.sale.out.SaleOutProductVo;
import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaleOutSheetServiceImplTest {

  /** 验证修改单据后，唯一报价模式与关闭模式的主表关键字段都会持久化。 */
  @Test
  void persistUpdatedSheetShouldPersistQuoteFieldsForBothPricingModes() throws Exception {
    SaleOutSheetMapper mapper = mock(SaleOutSheetMapper.class);
    when(mapper.updateById(any(SaleOutSheet.class))).thenReturn(1);
    SaleOutSheetServiceImpl service = new SaleOutSheetServiceImpl();
    setBaseMapper(service, mapper);

    SaleOutSheet quoteEnabled = new SaleOutSheet();
    quoteEnabled.setId("sheet-1");
    quoteEnabled.setOrderDate(LocalDate.of(2026, 9, 1));
    quoteEnabled.setQuoteSheetId("quote-2");
    quoteEnabled.setTotalAmount(new BigDecimal("25.00"));
    service.persistUpdatedSheet(quoteEnabled);

    SaleOutSheet quoteDisabled = new SaleOutSheet();
    quoteDisabled.setId("sheet-2");
    quoteDisabled.setOrderDate(LocalDate.of(2026, 9, 2));
    quoteDisabled.setQuoteSheetId(null);
    quoteDisabled.setTotalAmount(new BigDecimal("30.00"));
    service.persistUpdatedSheet(quoteDisabled);

    ArgumentCaptor<SaleOutSheet> captor = ArgumentCaptor.forClass(SaleOutSheet.class);
    verify(mapper, org.mockito.Mockito.times(2)).updateById(captor.capture());
    Assert.assertEquals(captor.getAllValues().get(0).getOrderDate(), LocalDate.of(2026, 9, 1));
    Assert.assertEquals(captor.getAllValues().get(0).getQuoteSheetId(), "quote-2");
    Assert.assertEquals(captor.getAllValues().get(0).getTotalAmount(), new BigDecimal("25.00"));
    Assert.assertNull(captor.getAllValues().get(1).getQuoteSheetId());
    Assert.assertEquals(captor.getAllValues().get(1).getOrderDate(), LocalDate.of(2026, 9, 2));
    Assert.assertEquals(captor.getAllValues().get(1).getTotalAmount(), new BigDecimal("30.00"));
  }

  /** 验证关闭唯一报价时，更新包装器显式将报价单ID设为数据库空值。 */
  @Test
  void clearPersistedQuoteSheetIdShouldExplicitlySetNullInUpdateWrapper() throws Exception {
    SaleOutSheetMapper mapper = mock(SaleOutSheetMapper.class);
    when(mapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
    SaleOutSheetServiceImpl service = new SaleOutSheetServiceImpl();
    setBaseMapper(service, mapper);
    initTableInfo(SaleOutSheet.class);

    service.clearPersistedQuoteSheetId("sheet-2");

    ArgumentCaptor<LambdaUpdateWrapper> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
    verify(mapper).update(isNull(), captor.capture());
    LambdaUpdateWrapper<SaleOutSheet> wrapper = captor.getValue();
    Assert.assertTrue(wrapper.getSqlSet().contains("quote_sheet_id"));
    Assert.assertTrue(wrapper.getParamNameValuePairs().containsValue(null));
  }

  /** 验证唯一报价会绑定主表并覆盖客户端提交的商品单价。 */
  @Test
  void resolveUniqueQuotePricesShouldBindSheetAndUseQuotePrices() {
    SaleOutSheet sheet = new SaleOutSheet();
    SaleOutProductVo product = new SaleOutProductVo();
    product.setSeq(1);
    product.setProductId("product-1");
    product.setTaxPrice(new BigDecimal("99"));
    QuoteProductBo quoteProduct = new QuoteProductBo();
    quoteProduct.setQuoteSheetId("quote-1");
    quoteProduct.setProductId("product-1");
    quoteProduct.setSalePrice(new BigDecimal("12.50"));

    Map<String, BigDecimal> prices = SaleOutSheetServiceImpl.resolveUniqueQuotePrices(sheet,
        Arrays.asList(product), Arrays.asList(quoteProduct));

    Assert.assertEquals(sheet.getQuoteSheetId(), "quote-1");
    Assert.assertEquals(prices.get("product-1"), new BigDecimal("12.50"));
  }

  /** 验证销售出库详情使用订单日期生效报价单回填询价商品标识。 */
  @Test
  void applyQuoteInquiryProductsShouldPopulateInquiryProductFromQuote() {
    SaleOutSheetFullDto sheet = new SaleOutSheetFullDto();
    SaleOutSheetFullDto.SheetDetailDto detail = new SaleOutSheetFullDto.SheetDetailDto();
    detail.setProductId("product-1");
    sheet.setDetails(Arrays.asList(detail));
    QuoteProductBo quoteProduct = new QuoteProductBo();
    quoteProduct.setProductId("product-1");
    quoteProduct.setInquiryProduct(true);

    SaleOutSheetServiceImpl.applyQuoteInquiryProducts(sheet, Arrays.asList(quoteProduct));

    Assert.assertTrue(sheet.getDetails().get(0).getInquiryProduct());
  }

  /** 验证销售商品不在当前报价单时拒绝保存。 */
  @Test(expectedExceptions = DefaultClientException.class,
      expectedExceptionsMessageRegExp = ".*不在当前生效报价单中.*")
  void resolveUniqueQuotePricesShouldRejectProductMissingFromQuote() {
    SaleOutSheet sheet = new SaleOutSheet();
    SaleOutProductVo product = new SaleOutProductVo();
    product.setSeq(2);
    product.setProductId("product-2");
    QuoteProductBo quoteProduct = new QuoteProductBo();
    quoteProduct.setQuoteSheetId("quote-1");
    quoteProduct.setProductId("product-1");
    quoteProduct.setSalePrice(BigDecimal.ONE);

    SaleOutSheetServiceImpl.resolveUniqueQuotePrices(sheet, Arrays.asList(product),
        Arrays.asList(quoteProduct));
  }

  /** 验证订单日期没有已启用报价单时拒绝保存。 */
  @Test(expectedExceptions = DefaultClientException.class,
      expectedExceptionsMessageRegExp = ".*不存在已启用报价单.*")
  void resolveUniqueQuotePricesShouldRejectMissingQuoteSheet() {
    SaleOutSheetServiceImpl.resolveUniqueQuotePrices(new SaleOutSheet(),
        Arrays.asList(new SaleOutProductVo()), Arrays.asList());
  }

  /** 验证标签打印数量会去除小数点后的无意义零。 */
  @Test
  void formatTagPrintNumShouldStripTrailingZero() {
    Assert.assertEquals(SaleOutSheetServiceImpl.formatTagPrintNum(new BigDecimal("1.0")), "1");
    Assert.assertEquals(SaleOutSheetServiceImpl.formatTagPrintNum(new BigDecimal("1.5")), "1.5");
  }

  @Test
  void normalizeQueryImportNumbersShouldConvertNullQuantityToZero() {
    SaleOutSheetQueryImportModel model = new SaleOutSheetQueryImportModel();
    model.setOrderNum(null);
    model.setConfirmNum(null);

    SaleOutSheetServiceImpl.normalizeQueryImportNumbers(model);

    Assert.assertEquals(model.getOrderNum(), BigDecimal.ZERO);
    Assert.assertEquals(model.getConfirmNum(), BigDecimal.ZERO);
  }

  @Test
  void validateImportNumbersShouldAllowNullValues() {
    SaleOutSheetImportModel model = createModel(null, null, null);

    Assert.assertTrue(SaleOutSheetServiceImpl.validateImportNumbers(model).isEmpty());
  }

  @Test
  void validateImportNumbersShouldAllowZeroValues() {
    SaleOutSheetImportModel model = createModel(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    Assert.assertTrue(SaleOutSheetServiceImpl.validateImportNumbers(model).isEmpty());
  }

  @Test
  void validateImportNumbersShouldRejectNegativeValues() {
    SaleOutSheetImportModel model = createModel(new BigDecimal("-0.00000001"), new BigDecimal("-0.000001"),
        BigDecimal.ZERO);

    List<String> errors = SaleOutSheetServiceImpl.validateImportNumbers(model);

    Assert.assertTrue(errors.contains("第2行“验收数量”不允许小于0"));
    Assert.assertTrue(errors.contains("第2行“单价”不允许小于0"));
  }

  @Test
  void validateImportNumbersShouldRetainPrecisionLimits() {
    SaleOutSheetImportModel model = createModel(new BigDecimal("1.123456789"), new BigDecimal("1.1234567"),
        BigDecimal.ZERO);

    List<String> errors = SaleOutSheetServiceImpl.validateImportNumbers(model);

    Assert.assertTrue(errors.contains("第2行“数量”最多允许8位小数"));
    Assert.assertTrue(errors.contains("第2行“单价”最多允许6位小数"));
  }

  @Test
  void validateImportNumbersShouldRejectNegativeConfirmNum() {
    SaleOutSheetImportModel model = createModel(BigDecimal.ONE, BigDecimal.ONE,
        new BigDecimal("-0.000001"));

    List<String> errors = SaleOutSheetServiceImpl.validateImportNumbers(model);

    Assert.assertTrue(errors.contains("第2行“验收数量”不允许小于0"));
  }

  @Test
  void validateImportNumbersShouldRejectConfirmNumWithMoreThanSixDecimals() {
    SaleOutSheetImportModel model = createModel(BigDecimal.ONE, BigDecimal.ONE,
        new BigDecimal("1.1234567"));

    List<String> errors = SaleOutSheetServiceImpl.validateImportNumbers(model);

    Assert.assertTrue(errors.contains("第2行“验收数量”最多允许6位小数"));
  }

  /**
   * 验证刷新成本时优先使用验收数量计算成本金额。
   */
  @Test
  void resolveCostNumShouldPreferPositiveConfirmNum() {
    SaleOutSheetDetail detail = new SaleOutSheetDetail();
    detail.setOrderNum(new BigDecimal("10"));
    detail.setConfirmNum(new BigDecimal("6.5"));

    Assert.assertEquals(SaleOutSheetServiceImpl.resolveCostNum(detail), new BigDecimal("6.5"));
  }

  /**
   * 验证刷新成本时验收数量为空才使用出库数量计算成本金额。
   */
  @Test
  void resolveCostNumShouldFallbackToOrderNumWhenConfirmNumNull() {
    SaleOutSheetDetail detail = new SaleOutSheetDetail();
    detail.setOrderNum(new BigDecimal("10"));

    Assert.assertEquals(SaleOutSheetServiceImpl.resolveCostNum(detail), new BigDecimal("10"));
  }

  /**
   * 验证刷新成本时验收数量为 0 也按验收数量计算成本金额。
   */
  @Test
  void resolveCostNumShouldUseZeroConfirmNum() {
    SaleOutSheetDetail detail = new SaleOutSheetDetail();
    detail.setOrderNum(new BigDecimal("10"));
    detail.setConfirmNum(BigDecimal.ZERO);

    Assert.assertEquals(SaleOutSheetServiceImpl.resolveCostNum(detail), new BigDecimal("10"));
  }

  /**
   * 验证合并单据ID会去除空值并去重。
   */
  @Test
  void normalizeMergeSheetIdsShouldRemoveBlankAndDuplicateIds() {
    List<String> ids = SaleOutSheetServiceImpl.normalizeMergeSheetIds(
        Arrays.asList("sale-1", "", "sale-2", "sale-1", null));

    Assert.assertEquals(ids, Arrays.asList("sale-1", "sale-2"));
  }

  /**
   * 验证合并单据至少需要两张有效单据。
   */
  @Test(expectedExceptions = DefaultClientException.class)
  void normalizeMergeSheetIdsShouldRejectLessThanTwoIds() {
    SaleOutSheetServiceImpl.normalizeMergeSheetIds(Arrays.asList("sale-1", "", "sale-1"));
  }

  /**
   * 验证合并时保留创建时间最早的单据。
   */
  @Test
  void sortMergeSheetsShouldPutEarliestCreateTimeFirst() {
    SaleOutSheet lateSheet = createSheet("sale-2", "SO-002",
        LocalDateTime.of(2026, 8, 12, 10, 0));
    SaleOutSheet earlySheet = createSheet("sale-1", "SO-001",
        LocalDateTime.of(2026, 8, 12, 9, 0));

    List<SaleOutSheet> sheets = Arrays.asList(lateSheet, earlySheet);

    SaleOutSheetServiceImpl.sortMergeSheets(sheets);

    Assert.assertEquals(sheets.get(0).getId(), "sale-1");
  }

  /**
   * 验证合并销售出库单不再限制订单日期一致。
   */
  @Test
  void validateMergeSheetsShouldAllowDifferentOrderDates() {
    SaleOutSheet target = createMergeSheet("sale-1", LocalDate.of(2026, 8, 12));
    SaleOutSheet other = createMergeSheet("sale-2", LocalDate.of(2026, 8, 13));

    new SaleOutSheetServiceImpl().validateMergeSheets(target, Arrays.asList(target, other));
  }

  /**
   * 验证合并订单时明细计划日期使用原订单日期。
   */
  @Test
  void toMergeProductVoShouldUseSourceOrderDateAsPlanDate() {
    SaleOutSheetDetail detail = new SaleOutSheetDetail();
    LocalDate orderDate = LocalDate.of(2026, 8, 13);

    SaleOutProductVo product = SaleOutSheetServiceImpl.toMergeProductVo(detail, orderDate, 1);

    Assert.assertEquals(product.getPlanDate(), orderDate);
  }

  /**
   * 验证标签打印只保留用户勾选的销售明细。
   */
  @Test
  void filterTagPrintDetailsShouldKeepSelectedDetailsOnly() {
    SaleOutSheetDetail firstDetail = new SaleOutSheetDetail();
    firstDetail.setId("detail-1");
    SaleOutSheetDetail secondDetail = new SaleOutSheetDetail();
    secondDetail.setId("detail-2");

    List<SaleOutSheetDetail> details = SaleOutSheetServiceImpl.filterTagPrintDetails(
        Arrays.asList(firstDetail, secondDetail), Arrays.asList("detail-2"));

    Assert.assertEquals(details.size(), 1);
    Assert.assertEquals(details.get(0).getId(), "detail-2");
  }

  /**
   * 验证开票明细按商品和单位汇总，并优先使用大于零的验收数量和金额。
   */
  @Test
  void buildInvoiceDetailExportModelsShouldGroupByProductAndUnit() {
    QuerySaleOutSheetDetailDto first = createInvoiceDetail("product-1", "盒", "2", "20", "1", "10");
    QuerySaleOutSheetDetailDto second = createInvoiceDetail("product-1", "盒", "3", "30", "2", "18");
    QuerySaleOutSheetDetailDto differentUnit = createInvoiceDetail("product-1", "件", "4", "40", "0", "0");

    List<SaleOutSheetInvoiceDetailExportModel> models =
        SaleOutSheetServiceImpl.buildInvoiceDetailExportModels(
            Arrays.asList(first, second, differentUnit), false);

    Assert.assertEquals(models.size(), 2);
    SaleOutSheetInvoiceDetailExportModel box = models.stream()
        .filter(item -> "盒".equals(item.getUnit()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    Assert.assertEquals(box.getQuantity(), new BigDecimal("3"));
    Assert.assertEquals(box.getAmount(), new BigDecimal("28"));
    Assert.assertEquals(box.getPrice(), new BigDecimal("9.333333"));
    Assert.assertEquals(box.getCategoryName(), "测试分类");
    SaleOutSheetInvoiceDetailExportModel piece = models.stream()
        .filter(item -> "件".equals(item.getUnit()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    Assert.assertEquals(piece.getQuantity(), new BigDecimal("4"));
    Assert.assertEquals(piece.getAmount(), new BigDecimal("40"));
    Assert.assertEquals(piece.getPrice(), new BigDecimal("10.000000"));
  }

  /**
   * 验证启用商品售价参数时，开票明细使用商品当前售价作为单价。
   */
  @Test
  void buildInvoiceDetailExportModelsShouldUseProductSalePriceWhenConfigured() {
    QuerySaleOutSheetDetailDto detail = createInvoiceDetail("product-1", "盒", "2", "20", "0", "0");
    detail.setProductSalePrice(new BigDecimal("12.50"));

    List<SaleOutSheetInvoiceDetailExportModel> models =
        SaleOutSheetServiceImpl.buildInvoiceDetailExportModels(Arrays.asList(detail), true);

    Assert.assertEquals(models.get(0).getPrice(), new BigDecimal("12.50"));
  }

  private SaleOutSheetImportModel createModel(BigDecimal orderNum, BigDecimal taxPrice,
      BigDecimal confirmNum) {
    SaleOutSheetImportModel model = new SaleOutSheetImportModel();
    model.setSeq(2);
    model.setOrderNum(orderNum);
    model.setTaxPrice(taxPrice);
    model.setConfirmNum(confirmNum);
    return model;
  }

  /** 为单元测试注入 BaseMpServiceImpl 持有的 Mapper。 */
  private void setBaseMapper(SaleOutSheetServiceImpl service, SaleOutSheetMapper mapper)
      throws Exception {
    Class<?> type = service.getClass();
    while (type != null) {
      try {
        Field field = type.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, mapper);
        return;
      } catch (NoSuchFieldException ignored) {
        type = type.getSuperclass();
      }
    }
    throw new AssertionError("未找到 BaseMapper 字段");
  }

  /** 初始化 LambdaUpdateWrapper 解析实体字段所需的 MyBatis 元数据。 */
  private void initTableInfo(Class<?> entityClass) {
    TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"),
        entityClass);
  }

  private SaleOutSheet createSheet(String id, String code, LocalDateTime createTime) {
    SaleOutSheet sheet = new SaleOutSheet();
    sheet.setId(id);
    sheet.setCode(code);
    sheet.setCreateTime(createTime);
    return sheet;
  }

  /**
   * 创建开票明细测试数据。
   *
   * @param productId 商品ID
   * @param unit 单位
   * @param orderNum 出库数量
   * @param taxAmount 销售金额
   * @param confirmNum 验收数量
   * @param confirmAmt 验收金额
   * @return 销售出库明细
   */
  private QuerySaleOutSheetDetailDto createInvoiceDetail(String productId, String unit, String orderNum,
      String taxAmount, String confirmNum, String confirmAmt) {
    QuerySaleOutSheetDetailDto detail = new QuerySaleOutSheetDetailDto();
    detail.setProductId(productId);
    detail.setProductCode("P-001");
    detail.setProductName("测试商品");
    detail.setCategoryName("测试分类");
    detail.setUnit(unit);
    detail.setOrderNum(new BigDecimal(orderNum));
    detail.setTaxAmount(new BigDecimal(taxAmount));
    detail.setConfirmNum(new BigDecimal(confirmNum));
    detail.setConfirmAmt(new BigDecimal(confirmAmt));
    return detail;
  }

  /**
   * 创建用于合并校验的销售出库单。
   *
   * @param id 销售出库单ID
   * @param orderDate 订单日期
   * @return 销售出库单
   */
  private SaleOutSheet createMergeSheet(String id, LocalDate orderDate) {
    SaleOutSheet sheet = new SaleOutSheet();
    sheet.setId(id);
    sheet.setCode(id);
    sheet.setCustomerId("customer-1");
    sheet.setScId("sc-1");
    sheet.setSaleOrderId("order-1");
    sheet.setOrderDate(orderDate);
    sheet.setStatus(SaleOutSheetStatus.CREATED);
    sheet.setSettleStatus(SettleStatus.UN_CHECK_BILL);
    return sheet;
  }
}
