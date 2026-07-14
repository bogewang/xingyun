package com.lframework.xingyun.basedata.impl.product;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.ProductUnit;
import com.lframework.xingyun.basedata.impl.product.ProductServiceImpl;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
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

  @Test(expectedExceptions = DefaultClientException.class,
      expectedExceptionsMessageRegExp = "商品已被业务单据或库存数据引用，无法删除！")
  void shouldRejectDeleteWhenProductHasBusinessReference() {
    ProductServiceImpl.assertNoProductReference("product-1",
        Collections.singletonList(productId -> true));
  }

  private Product product(String id, String unitId) {
    Product product = new Product();
    product.setId(id);
    product.setUnit(unitId);
    return product;
  }
}
