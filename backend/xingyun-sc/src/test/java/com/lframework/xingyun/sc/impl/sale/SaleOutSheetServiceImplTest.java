package com.lframework.xingyun.sc.impl.sale;

import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetImportModel;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetQueryImportModel;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import com.lframework.xingyun.sc.vo.sale.out.SaleOutProductVo;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

class SaleOutSheetServiceImplTest {

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

    Assert.assertTrue(errors.contains("第2行“数量”不允许小于0"));
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

    Assert.assertEquals(SaleOutSheetServiceImpl.resolveCostNum(detail), BigDecimal.ZERO);
  }

  /**
   * 验证保存销售出库单前先按商品类别、商品名称排序。
   */
  @Test
  void sortProductsForSaveShouldOrderByCategoryThenProductName() {
    Product categoryBProduct = createProduct("product-1", "商品A", "category-b");
    Product categoryAProductB = createProduct("product-2", "商品B", "category-a");
    Product categoryAProductA = createProduct("product-3", "商品A", "category-a");
    Map<String, Product> productMap = new HashMap<>();
    productMap.put(categoryBProduct.getId(), categoryBProduct);
    productMap.put(categoryAProductB.getId(), categoryAProductB);
    productMap.put(categoryAProductA.getId(), categoryAProductA);
    Map<String, String> categoryNameMap = new HashMap<>();
    categoryNameMap.put("category-a", "类别A");
    categoryNameMap.put("category-b", "类别B");

    List<SaleOutProductVo> sortedProducts = SaleOutSheetServiceImpl.sortProductsForSave(
        Arrays.asList(createProductVo("product-1", 1), createProductVo("product-2", 2),
            createProductVo("product-3", 3)), productMap, categoryNameMap);

    Assert.assertEquals(sortedProducts.get(0).getProductId(), "product-3");
    Assert.assertEquals(sortedProducts.get(1).getProductId(), "product-2");
    Assert.assertEquals(sortedProducts.get(2).getProductId(), "product-1");
  }

  /**
   * 创建商品测试数据。
   */
  private Product createProduct(String id, String name, String categoryId) {
    Product product = new Product();
    product.setId(id);
    product.setName(name);
    product.setCategoryId(categoryId);
    return product;
  }

  /**
   * 创建销售出库商品行测试数据。
   */
  private SaleOutProductVo createProductVo(String productId, int seq) {
    SaleOutProductVo productVo = new SaleOutProductVo();
    productVo.setProductId(productId);
    productVo.setSeq(seq);
    return productVo;
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
}
