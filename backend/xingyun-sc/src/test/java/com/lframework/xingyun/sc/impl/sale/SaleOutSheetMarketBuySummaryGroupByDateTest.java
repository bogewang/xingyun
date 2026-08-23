package com.lframework.xingyun.sc.impl.sale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 买菜汇总日期分组测试。
 */
class SaleOutSheetMarketBuySummaryGroupByDateTest {

  /**
   * 验证默认表头不包含日期列，勾选按日期汇总后才包含日期列。
   */
  @Test
  void headersShouldIncludeDateOnlyWhenGroupByDateEnabled() {
    Map<String, String> defaultHeaders = SaleOutSheetServiceImpl.buildMarketBuySummaryHeaders(false);
    Map<String, String> dateHeaders = SaleOutSheetServiceImpl.buildMarketBuySummaryHeaders(true);

    assertEquals(Arrays.asList("category", "productName", "spec", "total", "detail"),
        new ArrayList<>(defaultHeaders.keySet()));
    assertEquals("日期", dateHeaders.get("date"));
  }

  /**
   * 验证关闭日期汇总时跨日期商品使用同一行键，开启后使用不同行键。
   */
  @Test
  void rowKeyShouldMergeOrSeparateDifferentDatesAccordingToOption() {
    LocalDate firstDate = LocalDate.of(2026, 8, 1);
    LocalDate secondDate = LocalDate.of(2026, 8, 2);

    assertEquals(
        SaleOutSheetServiceImpl.buildMarketBuySummaryRowKey(firstDate, "product-1", false),
        SaleOutSheetServiceImpl.buildMarketBuySummaryRowKey(secondDate, "product-1", false));
    assertNotEquals(
        SaleOutSheetServiceImpl.buildMarketBuySummaryRowKey(firstDate, "product-1", true),
        SaleOutSheetServiceImpl.buildMarketBuySummaryRowKey(secondDate, "product-1", true));
  }

  /**
   * 验证关闭日期汇总时跨日期明细合并为一行，开启后拆分为两行。
   */
  @Test
  void summaryRowsShouldFollowGroupByDateOption() throws Exception {
    SaleOutSheet firstSheet = createSheet("sheet-1", LocalDate.of(2026, 8, 1));
    SaleOutSheet secondSheet = createSheet("sheet-2", LocalDate.of(2026, 8, 2));
    Product product = new Product();
    product.setId("product-1");
    product.setName("土豆");

    List<SaleOutSheetDetail> details = Arrays.asList(
        createDetail("sheet-1"), createDetail("sheet-2"));
    Map<String, SaleOutSheet> sheetMap = new HashMap<>();
    sheetMap.put(firstSheet.getId(), firstSheet);
    sheetMap.put(secondSheet.getId(), secondSheet);
    Map<String, Product> productMap = Collections.singletonMap(product.getId(), product);

    assertEquals(1, buildSummaryRows(details, sheetMap, productMap, false).size());
    assertEquals(2, buildSummaryRows(details, sheetMap, productMap, true).size());
  }

  /**
   * 验证同一商品始终汇总为一行；默认保留多段明细，勾选合并后归并为一段明细。
   */
  @Test
  void summaryRowsShouldMergeSameDaySameCustomerProductOnlyWhenEnabled() throws Exception {
    SaleOutSheet firstSheet = createSheet("sheet-1", LocalDate.of(2026, 8, 1));
    SaleOutSheet secondSheet = createSheet("sheet-2", LocalDate.of(2026, 8, 1));
    Product product = new Product();
    product.setId("product-1");
    product.setName("土豆");

    List<SaleOutSheetDetail> details = Arrays.asList(
        createDetail("detail-1", "sheet-1"), createDetail("detail-2", "sheet-2"));
    Map<String, SaleOutSheet> sheetMap = new HashMap<>();
    sheetMap.put(firstSheet.getId(), firstSheet);
    sheetMap.put(secondSheet.getId(), secondSheet);

    List<?> unmergedRows = buildSummaryRows(details, sheetMap,
        Collections.singletonMap(product.getId(), product), true, false);
    List<?> mergedRows = buildSummaryRows(details, sheetMap,
        Collections.singletonMap(product.getId(), product), true, true);

    assertEquals(1, unmergedRows.size());
    assertEquals(1, mergedRows.size());
    assertEquals(2, getCustomerDetailSize(unmergedRows.get(0)));
    assertEquals(1, getCustomerDetailSize(mergedRows.get(0)));
    assertEquals("【客户A】1+【客户A】1", buildCustomerDetail(unmergedRows.get(0)));
    assertEquals("【客户A】2", buildCustomerDetail(mergedRows.get(0)));
  }

  /**
   * 调用买菜汇总聚合方法生成测试结果。
   */
  private List<?> buildSummaryRows(List<SaleOutSheetDetail> details,
      Map<String, SaleOutSheet> sheetMap, Map<String, Product> productMap,
      boolean groupByDate) throws Exception {
    Method method = SaleOutSheetServiceImpl.class.getDeclaredMethod(
        "buildSummaryRows", List.class, Map.class, Map.class, Map.class, Map.class,
        boolean.class);
    method.setAccessible(true);
    return (List<?>) method.invoke(new SaleOutSheetServiceImpl(), details, sheetMap, productMap,
        Collections.emptyMap(), Collections.emptyMap(), groupByDate);
  }

  /**
   * 调用包含同日同客户商品合并选项的买菜汇总聚合方法。
   */
  private List<?> buildSummaryRows(List<SaleOutSheetDetail> details,
      Map<String, SaleOutSheet> sheetMap, Map<String, Product> productMap,
      boolean groupByDate, boolean mergeSameDayCustomerProduct) throws Exception {
    Method method = SaleOutSheetServiceImpl.class.getDeclaredMethod(
        "buildSummaryRows", List.class, Map.class, Map.class, Map.class, Map.class,
        boolean.class, boolean.class);
    method.setAccessible(true);
    return (List<?>) method.invoke(new SaleOutSheetServiceImpl(), details, sheetMap, productMap,
        Collections.emptyMap(), Collections.emptyMap(), groupByDate,
        mergeSameDayCustomerProduct);
  }

  /**
   * 获取买菜汇总行中用于明细数量展示的客户明细段数量。
   */
  private int getCustomerDetailSize(Object summaryRow) throws Exception {
    java.lang.reflect.Field field = summaryRow.getClass().getDeclaredField(
        "marketBuySummaryDetails");
    field.setAccessible(true);
    return ((Map<?, ?>) field.get(summaryRow)).size();
  }

  /**
   * 构建买菜汇总的明细数量文本。
   */
  private String buildCustomerDetail(Object summaryRow) throws Exception {
    Method method = SaleOutSheetServiceImpl.class.getDeclaredMethod(
        "buildMarketBuySummaryDetail", summaryRow.getClass(), LinkedHashMap.class);
    method.setAccessible(true);
    LinkedHashMap<String, String> customerNameMap = new LinkedHashMap<>();
    customerNameMap.put("customer-1", "客户A");
    return (String) method.invoke(new SaleOutSheetServiceImpl(), summaryRow, customerNameMap);
  }

  /**
   * 创建指定日期的销售出库单。
   */
  private SaleOutSheet createSheet(String id, LocalDate orderDate) {
    SaleOutSheet sheet = new SaleOutSheet();
    sheet.setId(id);
    sheet.setCustomerId("customer-1");
    sheet.setOrderDate(orderDate);
    return sheet;
  }

  /**
   * 创建指定销售出库单的商品明细。
   */
  private SaleOutSheetDetail createDetail(String sheetId) {
    return createDetail(null, sheetId);
  }

  /**
   * 创建指定明细ID及销售出库单的商品明细。
   */
  private SaleOutSheetDetail createDetail(String id, String sheetId) {
    SaleOutSheetDetail detail = new SaleOutSheetDetail();
    detail.setId(id);
    detail.setSheetId(sheetId);
    detail.setProductId("product-1");
    detail.setOrderNum(BigDecimal.ONE);
    return detail;
  }
}
