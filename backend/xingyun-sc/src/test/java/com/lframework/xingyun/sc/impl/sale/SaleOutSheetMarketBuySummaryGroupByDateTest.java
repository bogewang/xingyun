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
    SaleOutSheetDetail detail = new SaleOutSheetDetail();
    detail.setSheetId(sheetId);
    detail.setProductId("product-1");
    detail.setOrderNum(BigDecimal.ONE);
    return detail;
  }
}
