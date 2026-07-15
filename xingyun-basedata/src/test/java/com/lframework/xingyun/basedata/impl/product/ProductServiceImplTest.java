package com.lframework.xingyun.basedata.impl.product;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.xingyun.core.service.ProductDeleteReferenceChecker;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.ProductUnit;
import com.lframework.xingyun.basedata.impl.product.ProductServiceImpl;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

class ProductServiceImplTest {

  @Test
  void shouldCreateBaseUnitForUnconfiguredProduct() {
    List<ProductUnit> units = ProductServiceImpl.buildDefaultProductUnits(
        Collections.singletonList(product("product-1", "unit-1")), Collections.<String>emptySet(),
        Collections.singletonMap("unit-1", "瓶"));

    Assert.assertEquals(units.size(), 1);
    ProductUnit unit = units.get(0);
    Assert.assertEquals(unit.getProductId(), "product-1");
    Assert.assertEquals(unit.getUnitName(), "瓶");
    Assert.assertEquals(unit.getConversionRate(), BigDecimal.ONE);
    Assert.assertTrue(unit.getBaseUnit());
    Assert.assertTrue(unit.getAvailable());
    Assert.assertEquals(unit.getSortNo(), Integer.valueOf(0));
  }

  @Test
  void shouldNotCreateUnitForConfiguredProduct() {
    List<ProductUnit> units = ProductServiceImpl.buildDefaultProductUnits(
        Collections.singletonList(product("product-1", "unit-1")), Collections.singleton("product-1"),
        Collections.singletonMap("unit-1", "瓶"));

    Assert.assertTrue(units.isEmpty());
  }

  @Test(expectedExceptions = DefaultClientException.class)
  void shouldRejectMissingUnitDictionary() {
    ProductServiceImpl.buildDefaultProductUnits(Collections.singletonList(product("product-1", "unit-1")),
        Collections.<String>emptySet(), Collections.<String, String>emptyMap());
  }

  @Test
  void shouldRejectDeleteWhenProductIsReferenced() throws Exception {
    ProductDeleteReferenceChecker checker = Mockito.mock(ProductDeleteReferenceChecker.class);
    Mockito.when(checker.isReferenced("product-1")).thenReturn(true);

    ProductServiceImpl service = new ProductServiceImpl();
    Field field = ProductServiceImpl.class.getDeclaredField("productDeleteReferenceChecker");
    field.setAccessible(true);
    field.set(service, checker);

    try {
      service.deleteById("product-1");
      Assert.fail("商品被引用时不应执行删除");
    } catch (DefaultClientException e) {
      Assert.assertEquals(e.getMessage(), "商品已被采购或销售单据引用，不能删除！");
    }

    Mockito.verify(checker).isReferenced("product-1");
  }

  private Product product(String id, String unitId) {
    Product product = new Product();
    product.setId(id);
    product.setUnit(unitId);
    return product;
  }
}
