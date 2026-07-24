package com.lframework.xingyun.sc.bo.purchase.receive;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.sc.dto.purchase.PurchaseProductDto;
import com.lframework.xingyun.sc.dto.purchase.receive.ReceiveSheetFullDto;
import com.lframework.xingyun.sc.service.purchase.PurchaseOrderService;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.springframework.context.ApplicationContext;

/**
 * 采购入库打印模型字段测试。
 */
class PrintReceiveSheetBoTest {

  /**
   * 确认采购入库打印主表包含完整业务字段和已有展示字段。
   */
  @Test
  void shouldExposeCompleteReceiveSheetFields() {
    Set<String> fieldNames = getFieldNames(PrintReceiveSheetBo.class);
    Set<String> expectedFields = new HashSet<>(Arrays.asList(
        "id", "code", "scId", "supplierId", "purchaserId", "orderDate",
        "purchaseOrderId", "paymentDate", "receiveDate", "totalNum",
        "totalGiftNum", "totalAmount", "paidAmount", "unpaidAmount", "description",
        "createBy", "createTime", "updateBy", "updateTime", "approveBy", "approveTime",
        "status", "refuseReason", "settleStatus", "details", "scCode", "scName",
        "supplierCode", "supplierName", "purchaserName", "purchaseOrderCode"));

    Assert.assertTrue(fieldNames.containsAll(expectedFields),
        "采购入库打印主表缺少字段：" + difference(expectedFields, fieldNames));
  }

  /**
   * 确认采购入库打印明细包含原始业务字段和已有展示字段。
   */
  @Test
  void shouldExposeCompleteReceiveSheetDetailFields() {
    Set<String> fieldNames = getFieldNames(PrintReceiveSheetBo.OrderDetailBo.class);
    Set<String> expectedFields = new HashSet<>(Arrays.asList(
        "id", "productId", "orderNum", "unitId", "unitName", "conversionRate",
        "businessNum", "taxPrice", "taxAmount", "isGift", "taxRate", "description",
        "orderNo", "purchaseOrderDetailId", "productionDate", "productCode",
        "productName", "skuCode", "externalCode", "receiveNum", "purchasePrice",
        "receiveAmount", "inquiryProduct"));

    Assert.assertTrue(fieldNames.containsAll(expectedFields),
        "采购入库打印明细缺少字段：" + difference(expectedFields, fieldNames));
  }

  /**
   * 确认询价商品标识能从收货明细 DTO 原样映射到打印明细。
   */
  @Test
  void shouldMapInquiryProductToPrintDetail() throws ReflectiveOperationException {
    synchronized (ApplicationUtil.class) {
      ApplicationContext originalApplicationContext = getApplicationContext();
      ApplicationContext applicationContext = mock(ApplicationContext.class);
      PurchaseOrderService purchaseOrderService = mock(PurchaseOrderService.class);
      when(applicationContext.getBean(PurchaseOrderService.class)).thenReturn(purchaseOrderService);
      PurchaseProductDto product = new PurchaseProductDto();
      product.setCode("P001");
      product.setName("询价商品");
      when(purchaseOrderService.getPurchaseById("product-1")).thenReturn(product);
      new ApplicationUtil().setApplicationContext(applicationContext);

      try {
        ReceiveSheetFullDto.OrderDetailDto inquiryDetail = new ReceiveSheetFullDto.OrderDetailDto();
        inquiryDetail.setProductId("product-1");
        inquiryDetail.setInquiryProduct(true);
        ReceiveSheetFullDto.OrderDetailDto normalDetail = new ReceiveSheetFullDto.OrderDetailDto();
        normalDetail.setProductId("product-1");
        normalDetail.setInquiryProduct(false);

        Assert.assertTrue(
            new PrintReceiveSheetBo.OrderDetailBo(inquiryDetail).getInquiryProduct());
        Assert.assertFalse(
            new PrintReceiveSheetBo.OrderDetailBo(normalDetail).getInquiryProduct());
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
   * 确认测试字段集合覆盖业务 DTO 当前全部非静态字段，防止新增字段遗漏。
   */
  @Test
  void shouldKeepPrintModelAlignedWithReceiveSheetDto() {
    Set<String> printRootFields = getFieldNames(PrintReceiveSheetBo.class);
    Set<String> dtoRootFields = getFieldNames(ReceiveSheetFullDto.class);
    Set<String> printDetailFields = getFieldNames(PrintReceiveSheetBo.OrderDetailBo.class);
    Set<String> dtoDetailFields = getFieldNames(ReceiveSheetFullDto.OrderDetailDto.class);

    Assert.assertTrue(printRootFields.containsAll(dtoRootFields),
        "打印主表未覆盖业务 DTO 字段：" + difference(dtoRootFields, printRootFields));
    Assert.assertTrue(printDetailFields.containsAll(dtoDetailFields),
        "打印明细未覆盖业务 DTO 字段：" + difference(dtoDetailFields, printDetailFields));
  }

  /**
   * 获取指定类型声明的非静态字段名称。
   *
   * @param type 类型
   * @return 字段名称集合
   */
  private Set<String> getFieldNames(Class<?> type) {
    return Arrays.stream(type.getDeclaredFields())
        .filter(field -> !Modifier.isStatic(field.getModifiers()))
        .map(Field::getName)
        .collect(Collectors.toSet());
  }

  /**
   * 计算期望字段集合中缺失的字段。
   *
   * @param expectedFields 期望字段
   * @param actualFields 实际字段
   * @return 缺失字段
   */
  private Set<String> difference(Set<String> expectedFields, Set<String> actualFields) {
    Set<String> difference = new HashSet<>(expectedFields);
    difference.removeAll(actualFields);
    return difference;
  }
}
