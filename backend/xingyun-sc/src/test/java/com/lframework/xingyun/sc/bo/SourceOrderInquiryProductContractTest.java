package com.lframework.xingyun.sc.bo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.sc.bo.purchase.PurchaseOrderWithReceiveBo;
import com.lframework.xingyun.sc.bo.sale.SaleOrderWithOutBo;
import com.lframework.xingyun.sc.dto.purchase.PurchaseOrderWithReceiveDto;
import com.lframework.xingyun.sc.dto.purchase.PurchaseProductDto;
import com.lframework.xingyun.sc.dto.sale.SaleOrderWithOutDto;
import com.lframework.xingyun.sc.dto.sale.SaleProductDto;
import com.lframework.xingyun.sc.service.purchase.PurchaseOrderService;
import com.lframework.xingyun.sc.service.sale.SaleOrderService;
import com.lframework.xingyun.sc.service.stock.ProductStockService;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * 来源订单固定明细询价标识契约测试。
 */
class SourceOrderInquiryProductContractTest {

  /**
   * 验证采购商品查询选择并映射询价标识。
   */
  @Test
  void shouldSelectAndMapInquiryProductForPurchaseProduct() throws IOException {
    Configuration configuration = parsePurchaseOrderMapper();
    ResultMapping inquiryProductMapping = configuration
        .getResultMap("com.lframework.xingyun.sc.mappers.PurchaseOrderMapper.PurchaseProductDto")
        .getResultMappings()
        .stream()
        .filter(mapping -> "inquiryProduct".equals(mapping.getProperty()))
        .findFirst()
        .orElseThrow(AssertionError::new);

    assertEquals("inquiry_product", inquiryProductMapping.getColumn());
    assertTrue(buildPurchaseSql(configuration, "getPurchaseById").contains("NULL AS inquiry_product"));
  }

  /**
   * 验证 true 和 false 能从采购商品数据原样贯通到采购收货来源订单明细 BO。
   */
  @Test
  void shouldPropagateInquiryProductToPurchaseOrderWithReceiveDetail()
      throws ReflectiveOperationException {
    PurchaseProductDto inquiryProduct = createPurchaseProduct("purchase-true", true);
    PurchaseProductDto normalProduct = createPurchaseProduct("purchase-false", false);
    PurchaseOrderService purchaseOrderService = mock(PurchaseOrderService.class);
    when(purchaseOrderService.getPurchaseById("purchase-true")).thenReturn(inquiryProduct);
    when(purchaseOrderService.getPurchaseById("purchase-false")).thenReturn(normalProduct);

    withApplicationContext(purchaseOrderService, mock(SaleOrderService.class), () -> {
      assertTrue(new PurchaseOrderWithReceiveBo.DetailBo("sc-1",
          createPurchaseDetail("purchase-true")).getInquiryProduct());
      assertFalse(new PurchaseOrderWithReceiveBo.DetailBo("sc-1",
          createPurchaseDetail("purchase-false")).getInquiryProduct());
    });
  }

  /**
   * 验证 true 和 false 能从销售商品数据原样贯通到销售出库来源订单明细 BO。
   */
  @Test
  void shouldPropagateInquiryProductToSaleOrderWithOutDetail()
      throws ReflectiveOperationException {
    SaleProductDto inquiryProduct = createSaleProduct("sale-true", true);
    SaleProductDto normalProduct = createSaleProduct("sale-false", false);
    SaleOrderService saleOrderService = mock(SaleOrderService.class);
    when(saleOrderService.getSaleById("sale-true")).thenReturn(inquiryProduct);
    when(saleOrderService.getSaleById("sale-false")).thenReturn(normalProduct);

    withApplicationContext(mock(PurchaseOrderService.class), saleOrderService, () -> {
      assertTrue(new SaleOrderWithOutBo.DetailBo("sc-1",
          createSaleDetail("sale-true")).getInquiryProduct());
      assertFalse(new SaleOrderWithOutBo.DetailBo("sc-1",
          createSaleDetail("sale-false")).getInquiryProduct());
    });
  }

  /**
   * 解析真实采购订单 Mapper 配置。
   *
   * @return MyBatis 配置
   * @throws IOException Mapper 读取失败
   */
  private Configuration parsePurchaseOrderMapper() throws IOException {
    Configuration configuration = new Configuration();
    String resource = "mappers/purchase/PurchaseOrderMapper.xml";
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
      new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
    }
    return configuration;
  }

  /**
   * 生成采购商品查询 SQL。
   *
   * @param configuration MyBatis 配置
   * @param statementId 查询方法名
   * @return 规范化 SQL
   */
  private String buildPurchaseSql(Configuration configuration, String statementId) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("id", "product-1");
    BoundSql boundSql = configuration.getMappedStatement(
            "com.lframework.xingyun.sc.mappers.PurchaseOrderMapper." + statementId)
        .getBoundSql(parameters);
    return boundSql.getSql().replaceAll("\\s+", " ");
  }

  /**
   * 构造采购商品数据。
   *
   * @param id 商品 ID
   * @param inquiryProduct 是否询价商品
   * @return 采购商品数据
   */
  private PurchaseProductDto createPurchaseProduct(String id, boolean inquiryProduct) {
    PurchaseProductDto product = new PurchaseProductDto();
    product.setId(id);
    product.setInquiryProduct(inquiryProduct);
    return product;
  }

  /**
   * 构造销售商品数据。
   *
   * @param id 商品 ID
   * @param inquiryProduct 是否询价商品
   * @return 销售商品数据
   */
  private SaleProductDto createSaleProduct(String id, boolean inquiryProduct) {
    SaleProductDto product = new SaleProductDto();
    product.setId(id);
    product.setInquiryProduct(inquiryProduct);
    return product;
  }

  /**
   * 构造采购来源订单明细。
   *
   * @param productId 商品 ID
   * @return 采购来源订单明细
   */
  private PurchaseOrderWithReceiveDto.DetailDto createPurchaseDetail(String productId) {
    PurchaseOrderWithReceiveDto.DetailDto detail = new PurchaseOrderWithReceiveDto.DetailDto();
    detail.setProductId(productId);
    detail.setOrderNum(BigDecimal.ONE);
    detail.setReceiveNum(BigDecimal.ZERO);
    return detail;
  }

  /**
   * 构造销售来源订单明细。
   *
   * @param productId 商品 ID
   * @return 销售来源订单明细
   */
  private SaleOrderWithOutDto.DetailDto createSaleDetail(String productId) {
    SaleOrderWithOutDto.DetailDto detail = new SaleOrderWithOutDto.DetailDto();
    detail.setProductId(productId);
    detail.setOrderNum(BigDecimal.ONE);
    detail.setOutNum(BigDecimal.ZERO);
    return detail;
  }

  /**
   * 在隔离的测试 Spring 上下文中执行断言。
   *
   * @param purchaseOrderService 采购订单服务
   * @param saleOrderService 销售订单服务
   * @param assertion 断言逻辑
   * @throws ReflectiveOperationException 全局上下文访问失败
   */
  private void withApplicationContext(PurchaseOrderService purchaseOrderService,
      SaleOrderService saleOrderService, Runnable assertion)
      throws ReflectiveOperationException {
    synchronized (ApplicationUtil.class) {
      ApplicationContext originalApplicationContext = getApplicationContext();
      ApplicationContext applicationContext = mock(ApplicationContext.class);
      ProductStockService productStockService = mock(ProductStockService.class);
      when(applicationContext.getBean(PurchaseOrderService.class)).thenReturn(
          purchaseOrderService);
      when(applicationContext.getBean(SaleOrderService.class)).thenReturn(saleOrderService);
      when(applicationContext.getBean(ProductStockService.class)).thenReturn(productStockService);
      new ApplicationUtil().setApplicationContext(applicationContext);
      try {
        assertion.run();
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
}
