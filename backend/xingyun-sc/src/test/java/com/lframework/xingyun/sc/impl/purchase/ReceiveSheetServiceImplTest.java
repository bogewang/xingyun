package com.lframework.xingyun.sc.impl.purchase;

import com.lframework.starter.common.utils.BeanUtil;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.xingyun.basedata.bo.quote.QuoteProductBo;
import com.lframework.xingyun.sc.dto.purchase.PurchaseOrderWithReceiveDto;
import com.lframework.xingyun.sc.dto.purchase.receive.ReceiveSheetFullDto;
import com.lframework.xingyun.sc.excel.purchase.receive.ReceiveSheetImportModel;
import com.lframework.xingyun.sc.excel.purchase.receive.ReceiveSheetQueryImportModel;
import com.lframework.xingyun.sc.vo.purchase.receive.ReceiveProductVo;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

class ReceiveSheetServiceImplTest {

  /**
   * 验证导入模型中的询价商品标识能正确复制到VO。
   */
  @Test
  void beanUtilCopyShouldTransferInquiryProduct() {
    ReceiveSheetImportModel model = new ReceiveSheetImportModel();
    model.setInquiryProduct(Boolean.TRUE);

    ReceiveProductVo vo = BeanUtil.copyProperties(model, ReceiveProductVo.class);

    Assert.assertEquals(vo.getInquiryProduct(), Boolean.TRUE);
  }

  /**
   * 验证导入模型中的询价商品标识为 false 时也能正确复制。
   */
  @Test
  void beanUtilCopyShouldTransferFalseInquiryProduct() {
    ReceiveSheetImportModel model = new ReceiveSheetImportModel();
    model.setInquiryProduct(Boolean.FALSE);

    ReceiveProductVo vo = BeanUtil.copyProperties(model, ReceiveProductVo.class);

    Assert.assertEquals(vo.getInquiryProduct(), Boolean.FALSE);
  }

  @Test
  void normalizeQueryImportNumbersShouldConvertNullQuantityToZero() {
    ReceiveSheetQueryImportModel model = new ReceiveSheetQueryImportModel();
    model.setReceiveNum(null);

    ReceiveSheetServiceImpl.normalizeQueryImportNumbers(model);

    Assert.assertEquals(model.getReceiveNum(), BigDecimal.ZERO);
  }

  @Test
  void validateImportNumbersShouldAllowNullValues() {
    ReceiveSheetImportModel model = createModel(null, null);

    Assert.assertTrue(ReceiveSheetServiceImpl.validateImportNumbers(model).isEmpty());
  }

  @Test
  void validateImportNumbersShouldAllowZeroValues() {
    ReceiveSheetImportModel model = createModel(BigDecimal.ZERO, BigDecimal.ZERO);

    Assert.assertTrue(ReceiveSheetServiceImpl.validateImportNumbers(model).isEmpty());
  }

  @Test
  void validateImportNumbersShouldRejectNegativeValues() {
    ReceiveSheetImportModel model = createModel(new BigDecimal("-0.00000001"), new BigDecimal("-0.000001"));

    List<String> errors = ReceiveSheetServiceImpl.validateImportNumbers(model);

    Assert.assertTrue(errors.contains("第2行“数量”不允许小于0"));
    Assert.assertTrue(errors.contains("第2行“单价”不允许小于0"));
  }

  @Test
  void validateImportNumbersShouldRetainPrecisionLimits() {
    ReceiveSheetImportModel model = createModel(new BigDecimal("1.123456789"), new BigDecimal("1.1234567"));

    List<String> errors = ReceiveSheetServiceImpl.validateImportNumbers(model);

    Assert.assertTrue(errors.contains("第2行“数量”最多允许8位小数"));
    Assert.assertTrue(errors.contains("第2行“单价”最多允许6位小数"));
  }

  @Test
  void validateProductionDateShouldRequireStrictCalendarDate() {
    Assert.assertTrue(ReceiveSheetServiceImpl.validateProductionDate(null, "第2行").isEmpty());
    Assert.assertTrue(
        ReceiveSheetServiceImpl.validateProductionDate("2024.02.29", "第2行").isEmpty());
    Assert.assertEquals(
        ReceiveSheetServiceImpl.validateProductionDate("2026.02.30", "第2行").get(0),
        "第2行商品生产日期格式错误，应为yyyy.MM.dd且必须是有效日期");
    Assert.assertEquals(
        ReceiveSheetServiceImpl.validateProductionDate("2026-07-16", "第2行").get(0),
        "第2行商品生产日期格式错误，应为yyyy.MM.dd且必须是有效日期");
  }

  /** 验证采购入库商品不在订单日期生效报价单内时拒绝保存。 */
  @Test(expectedExceptions = DefaultClientException.class, expectedExceptionsMessageRegExp = ".*不在当前生效报价单中.*")
  void validateQuoteProductCoverageShouldRejectProductOutsideQuoteSheet() {
    ReceiveProductVo product = new ReceiveProductVo();
    product.setSeq(2);
    product.setProductId("product-outside");

    QuoteProductBo quoteProduct = new QuoteProductBo();
    quoteProduct.setQuoteSheetId("quote-1");
    quoteProduct.setProductId("product-in-quote");

    ReceiveSheetServiceImpl.validateQuoteProductCoverage(Collections.singletonList(product),
        Collections.singletonList(quoteProduct));
  }

  /** 验证采购入库商品位于订单日期生效报价单内时允许保存。 */
  @Test
  void validateQuoteProductCoverageShouldAllowProductInQuoteSheet() {
    ReceiveProductVo product = new ReceiveProductVo();
    product.setSeq(1);
    product.setProductId("product-1");

    QuoteProductBo quoteProduct = new QuoteProductBo();
    quoteProduct.setQuoteSheetId("quote-1");
    quoteProduct.setProductId("product-1");

    ReceiveSheetServiceImpl.validateQuoteProductCoverage(Collections.singletonList(product),
        Collections.singletonList(quoteProduct));
  }

  /** 验证导入匹配到商品后回填商品档案名称。 */
  @Test
  void importProductMatchShouldFillProductName() throws Exception {
    String source = new String(Files.readAllBytes(Paths.get(
        "src/main/java/com/lframework/xingyun/sc/impl/purchase/ReceiveSheetServiceImpl.java")),
        StandardCharsets.UTF_8);

    Assert.assertTrue(source.contains("data.setProductName(product.getName());"));
  }

  /** 验证采购入库导入按订单日期过滤非报价商品。 */
  @Test
  void importShouldFilterProductsOutsideActiveQuoteSheet() throws Exception {
    String source = new String(Files.readAllBytes(Paths.get(
        "src/main/java/com/lframework/xingyun/sc/impl/purchase/ReceiveSheetServiceImpl.java")),
        StandardCharsets.UTF_8);

    Assert.assertTrue(source.contains("checkImportData(list, orderDate)"));
    Assert.assertTrue(source.contains("quoteProductIds.contains(product.getId())"));
  }

  /** 验证采购入库详情按单据日期回填报价单中的询价商品标识，供修改页面正确回显。 */
  @Test
  void receiveDetailShouldPopulateInquiryProductFromActiveQuoteSheet() {
    ReceiveSheetFullDto sheet = new ReceiveSheetFullDto();
    ReceiveSheetFullDto.OrderDetailDto detail = new ReceiveSheetFullDto.OrderDetailDto();
    detail.setProductId("product-1");
    sheet.setDetails(Collections.singletonList(detail));

    ReceiveSheetServiceImpl.applyQuoteInquiryProducts(sheet,
        Collections.singletonList(createQuoteProduct("product-1", true)));

    Assert.assertEquals(detail.getInquiryProduct(), Boolean.TRUE);
  }

  /** 验证新增采购入库从采购订单带入商品时保留询价商品标识。 */
  @Test
  void purchaseOrderForReceiveShouldPopulateInquiryProductFromActiveQuoteSheet() {
    PurchaseOrderWithReceiveDto order = new PurchaseOrderWithReceiveDto();
    PurchaseOrderWithReceiveDto.DetailDto detail = new PurchaseOrderWithReceiveDto.DetailDto();
    detail.setProductId("product-1");
    order.setDetails(Collections.singletonList(detail));

    PurchaseOrderServiceImpl.applyQuoteInquiryProducts(order,
        Collections.singletonList(createQuoteProduct("product-1", true)));

    Assert.assertEquals(detail.getInquiryProduct(), Boolean.TRUE);
  }

  /** 验证采购入库 Excel 导入按订单日期回填报价单中的询价商品标识。 */
  @Test
  void importShouldPopulateInquiryProductFromActiveQuoteSheet() throws Exception {
    String source = new String(Files.readAllBytes(Paths.get(
        "src/main/java/com/lframework/xingyun/sc/impl/purchase/ReceiveSheetServiceImpl.java")),
        StandardCharsets.UTF_8);

    Assert.assertTrue(source.contains("Map<String, QuoteProductBo> quoteProductMap"));
    Assert.assertTrue(source.contains("data.setInquiryProduct(quoteProduct == null ? null"));
  }

  /** 验证采购入库修改以版本号防止并发覆盖。 */
  @Test
  void updateShouldUseVersionForOptimisticLock() throws Exception {
    String source = new String(Files.readAllBytes(Paths.get(
        "src/main/java/com/lframework/xingyun/sc/impl/purchase/ReceiveSheetServiceImpl.java")),
        StandardCharsets.UTF_8);

    Assert.assertTrue(source.contains("sheet.setVersion(vo.getVersion() + 1);"));
    Assert.assertTrue(source.contains(".eq(ReceiveSheet::getVersion, vo.getVersion())"));
  }

  /** 构造报价商品。 */
  private QuoteProductBo createQuoteProduct(String productId, Boolean inquiryProduct) {
    QuoteProductBo quoteProduct = new QuoteProductBo();
    quoteProduct.setProductId(productId);
    quoteProduct.setInquiryProduct(inquiryProduct);
    return quoteProduct;
  }

  private ReceiveSheetImportModel createModel(BigDecimal receiveNum, BigDecimal purchasePrice) {
    ReceiveSheetImportModel model = new ReceiveSheetImportModel();
    model.setSeq(2);
    model.setReceiveNum(receiveNum);
    model.setPurchasePrice(purchasePrice);
    return model;
  }
}
