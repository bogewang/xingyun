package com.lframework.xingyun.basedata.impl.product;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.web.core.event.DataChangeEventBuilder;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.ProductUnit;
import com.lframework.xingyun.basedata.impl.product.ProductServiceImpl;
import com.lframework.xingyun.basedata.mappers.ProductMapper;
import com.lframework.xingyun.core.service.ProductDeleteReferenceChecker;
import com.lframework.xingyun.basedata.events.DeleteProductEvent;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
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

  /**
   * 验证商品已被业务单据引用时拒绝删除。
   */
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

  /**
   * 验证商品未被引用时按顺序执行状态更新。
   */
  @Test
  void shouldDeleteWhenProductIsNotReferenced() throws Exception {
    ProductDeleteReferenceChecker checker = Mockito.mock(ProductDeleteReferenceChecker.class);
    Mockito.when(checker.isReferenced("product-1")).thenReturn(false);
    ProductMapper mapper = Mockito.mock(ProductMapper.class);
    Product product = product("product-1", "unit-1");
    Mockito.when(mapper.selectById("product-1")).thenReturn(product);

    ProductServiceImpl service = new ProductServiceImpl();
    setField(service, "productDeleteReferenceChecker", checker);
    setField(service, "baseMapper", mapper);
    TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"),
        Product.class);

    try (MockedStatic<DataChangeEventBuilder> eventBuilder = Mockito.mockStatic(DataChangeEventBuilder.class)) {
      service.deleteById("product-1");
      eventBuilder.verify(() -> DataChangeEventBuilder.publishLogicDelete(service,
          DeleteProductEvent.class, product));
    }

    InOrder inOrder = Mockito.inOrder(checker, mapper);
    inOrder.verify(checker).isReferenced("product-1");
    inOrder.verify(mapper).update(Mockito.any());
    inOrder.verify(mapper).selectById("product-1");
    Mockito.verify(mapper, Mockito.times(1)).update(Mockito.any());
  }

  /**
   * 在对象继承层级中设置指定字段。
   *
   * @param target 目标对象
   * @param fieldName 字段名称
   * @param value 字段值
   * @throws Exception 未找到字段或无法设置字段时抛出
   */
  private void setField(Object target, String fieldName, Object value) throws Exception {
    Class<?> type = target.getClass();
    while (type != null) {
      try {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
        return;
      } catch (NoSuchFieldException e) {
        type = type.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName);
  }

  /**
   * 构造测试用商品。
   *
   * @param id 商品 ID
   * @param unitId 主单位 ID
   * @return 测试商品
   */
  private Product product(String id, String unitId) {
    Product product = new Product();
    product.setId(id);
    product.setUnit(unitId);
    return product;
  }
}
