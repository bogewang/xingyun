package com.lframework.xingyun.sc.bo.purchase.receive;

import com.lframework.xingyun.sc.dto.purchase.receive.ReceiveSheetFullDto;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.Test;

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
        "receiveAmount"));

    Assert.assertTrue(fieldNames.containsAll(expectedFields),
        "采购入库打印明细缺少字段：" + difference(expectedFields, fieldNames));
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
