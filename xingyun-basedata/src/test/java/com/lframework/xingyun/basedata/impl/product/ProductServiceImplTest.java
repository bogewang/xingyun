package com.lframework.xingyun.basedata.impl.product;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.ProductUnit;
import com.lframework.xingyun.basedata.mappers.ProductMapper;
import com.lframework.xingyun.basedata.service.product.ProductReferenceChecker;
import com.lframework.xingyun.basedata.service.product.ProductService;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.context.support.StaticApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

class ProductServiceImplTest {

  @Test
  void shouldKeepInquiryProductOnProductEntity() {
    Product product = new Product();
    product.setInquiryProduct(Boolean.TRUE);
    Assert.assertTrue(product.getInquiryProduct());
  }

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

  @Test
  void shouldPhysicallyDeleteUnreferencedProduct() throws Exception {
    RecordingProductMapper recordingMapper = new RecordingProductMapper();
    ProductServiceImpl service = new ProductServiceImpl();
    initializeApplicationContext();
    setProductReferenceCheckers(service, Collections.singletonList(productId -> false));
    setBaseMapper(service, recordingMapper.mapper());
    setProductService(service, new RecordingProductService().proxy());

    service.deleteById("product-1");

    Assert.assertEquals(recordingMapper.getDeleteByIdCount(), 1);
    Assert.assertEquals(recordingMapper.getUpdateCount(), 0);
  }

  @Test
  void shouldDelegateCacheEvictionToServiceProxyWhenDeletingProduct() throws Exception {
    RecordingProductMapper recordingMapper = new RecordingProductMapper();
    ProductServiceImpl service = new ProductServiceImpl();
    RecordingProductService recordingService = new RecordingProductService();
    initializeApplicationContext();
    setProductReferenceCheckers(service, Collections.singletonList(productId -> false));
    setBaseMapper(service, recordingMapper.mapper());
    setProductService(service, recordingService.proxy());

    service.deleteById("product-1");

    Assert.assertEquals(recordingService.getCleanCacheByKeyCount(), 1);
  }

  /**
   * 初始化逻辑删除事件发布所需的应用上下文。
   */
  private void initializeApplicationContext() {
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    applicationContext.refresh();
    new ApplicationUtil().setApplicationContext(applicationContext);
  }

  /**
   * 为服务注入商品引用检查器，隔离 Spring 容器。
   *
   * @param service 商品服务
   * @param checkers 商品引用检查器
   * @throws Exception 反射设置失败时抛出
   */
  private void setProductReferenceCheckers(ProductServiceImpl service, List<ProductReferenceChecker> checkers)
      throws Exception {
    Field field = ProductServiceImpl.class.getDeclaredField("productReferenceCheckers");
    field.setAccessible(true);
    field.set(service, checkers);
  }

  /**
   * 为服务注入记录调用的 Mapper。
   *
   * @param service 商品服务
   * @param mapper 商品 Mapper
   * @throws Exception 反射设置失败时抛出
   */
  private void setBaseMapper(ProductServiceImpl service, ProductMapper mapper) throws Exception {
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
    throw new IllegalStateException("未找到 BaseMapper 字段");
  }

  /**
   * 为服务注入缓存切面代理。
   *
   * @param service 商品服务
   * @param productService 商品服务代理
   * @throws Exception 反射设置失败时抛出
   */
  private void setProductService(ProductServiceImpl service, ProductService productService) throws Exception {
    Field field = ProductServiceImpl.class.getDeclaredField("productService");
    field.setAccessible(true);
    field.set(service, productService);
  }

  /**
   * 记录商品 Mapper 的删除及更新调用。
   */
  private static class RecordingProductMapper {

    private int deleteByIdCount;
    private int updateCount;

    /**
     * 创建记录调用的商品 Mapper 代理。
     *
     * @return 商品 Mapper 代理
     */
    private ProductMapper mapper() {
      return (ProductMapper) Proxy.newProxyInstance(ProductMapper.class.getClassLoader(),
          new Class<?>[] {ProductMapper.class}, (proxy, method, args) -> {
            if ("deleteById".equals(method.getName())) {
              deleteByIdCount++;
              return 1;
            }
            if ("update".equals(method.getName())) {
              updateCount++;
              return 1;
            }
            if ("selectById".equals(method.getName())) {
              return product("product-1", "unit-1");
            }
            if ("toString".equals(method.getName())) {
              return "RecordingProductMapper";
            }
            throw new UnsupportedOperationException("未预期的 Mapper 调用：" + method.getName()
                + Arrays.toString(args));
          });
    }

    /**
     * 获取物理删除调用次数。
     *
     * @return 物理删除调用次数
     */
    private int getDeleteByIdCount() {
      return deleteByIdCount;
    }

    /**
     * 获取更新调用次数。
     *
     * @return 更新调用次数
     */
    private int getUpdateCount() {
      return updateCount;
    }
  }

  /**
   * 记录缓存清理调用的商品服务代理。
   */
  private static class RecordingProductService {

    private int cleanCacheByKeyCount;

    /**
     * 创建记录缓存清理调用的商品服务代理。
     *
     * @return 商品服务代理
     */
    private ProductService proxy() {
      return (ProductService) Proxy.newProxyInstance(ProductService.class.getClassLoader(),
          new Class<?>[] {ProductService.class}, (proxy, method, args) -> {
            if ("cleanCacheByKey".equals(method.getName())) {
              cleanCacheByKeyCount++;
              return null;
            }
            if ("toString".equals(method.getName())) {
              return "RecordingProductService";
            }
            throw new UnsupportedOperationException("未预期的商品服务调用：" + method.getName()
                + Arrays.toString(args));
          });
    }

    /**
     * 获取缓存清理调用次数。
     *
     * @return 缓存清理调用次数
     */
    private int getCleanCacheByKeyCount() {
      return cleanCacheByKeyCount;
    }
  }

  private static Product product(String id, String unitId) {
    Product product = new Product();
    product.setId(id);
    product.setUnit(unitId);
    return product;
  }
}
