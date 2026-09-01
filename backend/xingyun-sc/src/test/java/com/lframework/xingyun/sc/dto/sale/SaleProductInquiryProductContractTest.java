package com.lframework.xingyun.sc.dto.sale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.service.product.ProductLatestPriceCacheService;
import com.lframework.xingyun.sc.bo.sale.SaleProductBo;
import com.lframework.xingyun.sc.service.stock.ProductStockService;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * 销售商品询价标识端点契约测试。
 */
class SaleProductInquiryProductContractTest {

  /**
   * 构造销售商品 BO 所需的测试 Spring 上下文。
   *
   * @return 测试 Spring 上下文
   */
  private ApplicationContext createApplicationContext() {
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    ProductLatestPriceCacheService latestPriceService = mock(
        ProductLatestPriceCacheService.class);
    ProductStockService productStockService = mock(ProductStockService.class);
    when(applicationContext.getBean(ProductLatestPriceCacheService.class)).thenReturn(
        latestPriceService);
    when(applicationContext.getBean(ProductStockService.class)).thenReturn(productStockService);
    when(productStockService.getByProductIdAndScId(any(), any())).thenReturn(null);
    return applicationContext;
  }

  /**
   * 验证商品联想和批量列表 SQL 均选择并映射询价标识。
   */
  @Test
  void shouldSelectInquiryProductForSearchAndListEndpoints() throws IOException {
    Configuration configuration = parseSaleOrderMapper();
    ResultMapping inquiryProductMapping = configuration
        .getResultMap("com.lframework.xingyun.sc.mappers.SaleOrderMapper.SaleProductDto")
        .getResultMappings()
        .stream()
        .filter(mapping -> "inquiryProduct".equals(mapping.getProperty()))
        .findFirst()
        .orElseThrow(AssertionError::new);

    assertEquals("inquiry_product", inquiryProductMapping.getColumn());
    assertTrue(buildSql(configuration, "querySaleByCondition",
        searchParameters()).contains("NULL AS inquiry_product"));
    assertTrue(buildSql(configuration, "querySaleList",
        listParameters()).contains("NULL AS inquiry_product"));
  }

  /**
   * 验证商品联想和批量列表 SQL 均选择并映射商品备注。
   */
  @Test
  void shouldSelectRemarkForSearchAndListEndpoints() throws IOException {
    Configuration configuration = parseSaleOrderMapper();
    ResultMapping remarkMapping = configuration
        .getResultMap("com.lframework.xingyun.sc.mappers.SaleOrderMapper.SaleProductDto")
        .getResultMappings()
        .stream()
        .filter(mapping -> "remark".equals(mapping.getProperty()))
        .findFirst()
        .orElseThrow(AssertionError::new);

    assertEquals("remark", remarkMapping.getColumn());
    assertTrue(buildSql(configuration, "querySaleByCondition",
        searchParameters()).contains("g.remark"));
    assertTrue(buildSql(configuration, "querySaleList",
        listParameters()).contains("g.remark"));
  }

  /**
   * 验证 true 和 false 能从销售商品 DTO 原样贯通到 API BO。
   */
  @Test
  void shouldPropagateTrueAndFalseFromDtoToBo() throws ReflectiveOperationException {
    synchronized (ApplicationUtil.class) {
      ApplicationContext originalApplicationContext = getApplicationContext();
      new ApplicationUtil().setApplicationContext(createApplicationContext());
      try {
        SaleProductDto inquiryProduct = createProduct(true);
        SaleProductDto normalProduct = createProduct(false);

        assertTrue(new SaleProductBo("sc-1", inquiryProduct).getInquiryProduct());
        assertFalse(new SaleProductBo("sc-1", normalProduct).getInquiryProduct());
      } finally {
        new ApplicationUtil().setApplicationContext(originalApplicationContext);
      }
    }
  }

  /**
   * 验证商品备注能从销售商品 DTO 贯通到 API BO。
   */
  @Test
  void shouldPropagateRemarkFromDtoToBo() throws ReflectiveOperationException {
    synchronized (ApplicationUtil.class) {
      ApplicationContext originalApplicationContext = getApplicationContext();
      new ApplicationUtil().setApplicationContext(createApplicationContext());
      try {
        SaleProductDto product = createProduct(false);
        product.setRemark("易碎品");

        assertEquals("易碎品", new SaleProductBo("sc-1", product).getRemark());
      } finally {
        new ApplicationUtil().setApplicationContext(originalApplicationContext);
      }
    }
  }

  /**
   * 获取测试开始前的全局 Spring 上下文。
   *
   * @return 当前全局 Spring 上下文
   * @throws ReflectiveOperationException 反射读取失败
   */
  private ApplicationContext getApplicationContext() throws ReflectiveOperationException {
    Field field = ApplicationUtil.class.getDeclaredField("APPLICATION_CONTEXT");
    field.setAccessible(true);
    return (ApplicationContext) field.get(null);
  }

  /**
   * 解析真实销售订单 Mapper 配置。
   *
   * @return MyBatis 配置
   * @throws IOException Mapper 读取失败
   */
  private Configuration parseSaleOrderMapper() throws IOException {
    Configuration configuration = new Configuration();
    String resource = "mappers/sale/SaleOrderMapper.xml";
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
      new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
    }
    return configuration;
  }

  /**
   * 生成指定 Mapper 查询的 SQL。
   *
   * @param configuration MyBatis 配置
   * @param statementId 查询方法名
   * @param parameters 查询参数
   * @return 规范化 SQL
   */
  private String buildSql(Configuration configuration, String statementId,
      Map<String, Object> parameters) {
    BoundSql boundSql = configuration.getMappedStatement(
            "com.lframework.xingyun.sc.mappers.SaleOrderMapper." + statementId)
        .getBoundSql(parameters);
    return boundSql.getSql().replaceAll("\\s+", " ");
  }

  /**
   * 构造商品联想查询参数。
   *
   * @return 查询参数
   */
  private Map<String, Object> searchParameters() {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("scId", "sc-1");
    parameters.put("condition", "商品");
    parameters.put("isReturn", false);
    return parameters;
  }

  /**
   * 构造批量商品列表查询参数。
   *
   * @return 查询参数
   */
  private Map<String, Object> listParameters() {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("scId", "sc-1");
    parameters.put("vo", new HashMap<>());
    return parameters;
  }

  /**
   * 构造带询价标识的销售商品 DTO。
   *
   * @param inquiryProduct 是否询价商品
   * @return 销售商品 DTO
   */
  private SaleProductDto createProduct(boolean inquiryProduct) {
    SaleProductDto product = new SaleProductDto();
    product.setId("product-" + inquiryProduct);
    product.setInquiryProduct(inquiryProduct);
    return product;
  }
}
