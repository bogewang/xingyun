package com.lframework.xingyun.sc.impl.sale;

import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import org.testng.Assert;
import org.testng.annotations.Test;

class SaleOutSheetMarketBuySummaryFormatterTest {

  /**
   * 验证备注已包含拆分数量时，不在括号内重复追加明细总数。
   */
  @Test
  void formatCustomerDetailShouldNotRepeatQuantityBeforeDetailedDescription() {
    Map<String, BigDecimal> quantityByDescription = new LinkedHashMap<>();
    quantityByDescription.put("0.5沫/1沫/0.5片/0.5片", new BigDecimal("2.5"));

    String result = SaleOutSheetMarketBuySummaryFormatter.formatCustomerDetailByDescription(
        "17灶", "kg", BigDecimal.ZERO, quantityByDescription);

    Assert.assertEquals(result, "【17灶】2.5kg（0.5沫/1沫/0.5片/0.5片）");
  }

  /**
   * 验证同一客户的多条商品明细保留各自数量与备注的对应关系。
   */
  @Test
  void buildMarketBuySummaryDetailShouldKeepQuantityMatchedWithEachRemark() throws Exception {
    SaleOutSheet sheet = new SaleOutSheet();
    sheet.setId("sheet-1");
    sheet.setCustomerId("customer-1");
    sheet.setOrderDate(LocalDate.of(2026, 8, 1));

    Product product = new Product();
    product.setId("product-1");
    product.setName("五花肉");
    product.setUnit("unit-1");

    List<SaleOutSheetDetail> details = Arrays.asList(
        createDetail("3", "去皮，切片"),
        createDetail("3", "整块"),
        createDetail("2.5", "切块"));
    Map<String, SaleOutSheet> sheetMap = Collections.singletonMap(sheet.getId(), sheet);
    Map<String, Product> productMap = Collections.singletonMap(product.getId(), product);
    Map<String, String> unitMap = Collections.singletonMap(product.getUnit(), "kg");

    SaleOutSheetServiceImpl service = new SaleOutSheetServiceImpl();
    Method buildSummaryRows = SaleOutSheetServiceImpl.class.getDeclaredMethod(
        "buildSummaryRows", List.class, Map.class, Map.class, Map.class, Map.class);
    buildSummaryRows.setAccessible(true);
    List<?> rows = (List<?>) buildSummaryRows.invoke(service, details, sheetMap, productMap,
        new HashMap<>(), unitMap);

    Class<?> summaryRowClass = Class.forName(
        SaleOutSheetServiceImpl.class.getName() + "$SummaryRow");
    Method buildDetail = SaleOutSheetServiceImpl.class.getDeclaredMethod(
        "buildMarketBuySummaryDetail", summaryRowClass, LinkedHashMap.class);
    buildDetail.setAccessible(true);
    LinkedHashMap<String, String> customers = new LinkedHashMap<>();
    customers.put("customer-1", "17灶");

    Assert.assertEquals(buildDetail.invoke(service, rows.get(0), customers),
        "【17灶】8.5/kg（3/去皮，切片；3/整块；2.5/切块）");
  }

  /**
   * 验证动态客户列只保留数量和去重后的备注，不包含客户名称或单位。
   */
  @Test
  void formatCustomerQuantityShouldKeepQuantityAndRemarksWithoutCustomerOrUnit() {
    Assert.assertEquals(SaleOutSheetMarketBuySummaryFormatter.formatCustomerQuantity(
        new BigDecimal("0.1"), Arrays.asList("2条大", "2条大", "切好")), "0.1（2条大；切好）");
  }

  /**
   * 验证数量为零且没有备注时动态客户列为空。
   */
  @Test
  void formatCustomerQuantityShouldReturnEmptyWhenQuantityAndRemarksAreEmpty() {
    Assert.assertEquals(SaleOutSheetMarketBuySummaryFormatter.formatCustomerQuantity(
        BigDecimal.ZERO, Collections.emptyList()), "");
  }

  /**
   * 验证数量为零但存在备注时动态客户列保留备注。
   */
  @Test
  void formatCustomerQuantityShouldRetainRemarksWhenQuantityIsZero() {
    Assert.assertEquals(SaleOutSheetMarketBuySummaryFormatter.formatCustomerQuantity(
        BigDecimal.ZERO, Collections.singletonList("补送")), "（补送）");
  }

  /**
   * 验证客户昵称非空时优先使用昵称，昵称为空白时回退到客户名称。
   */
  @Test
  void resolveCustomerNameShouldPreferNicknameAndFallbackToName() {
    Customer customer = new Customer();
    customer.setName("客户名称");
    customer.setNickName("客户昵称");
    Assert.assertEquals(SaleOutSheetMarketBuySummaryFormatter.resolveCustomerName(customer), "客户昵称");

    customer.setNickName(null);
    Assert.assertEquals(SaleOutSheetMarketBuySummaryFormatter.resolveCustomerName(customer), "客户名称");

    customer.setNickName("");
    Assert.assertEquals(SaleOutSheetMarketBuySummaryFormatter.resolveCustomerName(customer), "客户名称");

    customer.setNickName("  ");
    Assert.assertEquals(SaleOutSheetMarketBuySummaryFormatter.resolveCustomerName(customer), "客户名称");
  }

  /**
   * 验证客户明细包含单位，并按原始顺序去重拼接备注。
   */
  @Test
  void formatCustomerDetailShouldAppendUnitAndDeduplicatedRemarks() {
    Collection<String> descriptions = Arrays.asList("送老张", "送老张", "分两袋");

    String result = SaleOutSheetMarketBuySummaryFormatter.formatCustomerDetail(
        "绿春56", "公斤", new BigDecimal("3.5"), descriptions);

    Assert.assertEquals(result, "(绿春56)3.5/公斤（送老张；分两袋）");
  }

  /**
   * 验证多个客户使用加号连接，并忽略没有数量和备注的空明细。
   */
  @Test
  void mergeCustomerDetailsShouldKeepCustomerOrderAndOmitEmptyDetails() {
    List<SaleOutSheetMarketBuySummaryFormatter.CustomerDetail> details = Arrays.asList(
        new SaleOutSheetMarketBuySummaryFormatter.CustomerDetail(
            "绿春56", "公斤", new BigDecimal("1.5"), Collections.emptyList()),
        new SaleOutSheetMarketBuySummaryFormatter.CustomerDetail(
            "平河57", "公斤", new BigDecimal("2"), Collections.singletonList("上午送达")),
        new SaleOutSheetMarketBuySummaryFormatter.CustomerDetail(
            "空客户", "公斤", BigDecimal.ZERO, Collections.emptyList()));

    String result = SaleOutSheetMarketBuySummaryFormatter.mergeCustomerDetails(details);

    Assert.assertEquals(result, "(绿春56)1.5/公斤+(平河57)2/公斤（上午送达）");
  }

  /**
   * 验证数量为零但存在备注时仍保留客户和备注信息。
   */
  @Test
  void formatCustomerDetailShouldRetainDescriptionWhenQuantityIsZero() {
    String result = SaleOutSheetMarketBuySummaryFormatter.formatCustomerDetail(
        "客户", "个", BigDecimal.ZERO, Collections.singletonList("补送"));

    Assert.assertEquals(result, "(客户)（补送）");
  }

  /**
   * 验证买菜汇总使用固定的单列表头。
   */
  @Test
  void buildMarketBuySummaryHeadersShouldUseOneDetailColumn() {
    Map<String, String> headers = SaleOutSheetServiceImpl.buildMarketBuySummaryHeaders();
    Map<String, String> expectedHeaders = new LinkedHashMap<>();
    expectedHeaders.put("date", "日期");
    expectedHeaders.put("productName", "商品名称");
    expectedHeaders.put("category", "分类名称");
    expectedHeaders.put("total", "总重量");
    expectedHeaders.put("detail", "明细数量");

    Assert.assertEquals(headers, expectedHeaders);
    Assert.assertEquals(new ArrayList<>(headers.keySet()), Arrays.asList(
        "date", "productName", "category", "total", "detail"));
  }

  /**
   * 验证买菜汇总2将日期置于首列，并在总计列前按客户顺序生成动态列。
   */
  @Test
  void buildMarketBuySummary2HeadersShouldPutDateFirstAndCustomersBeforeTotal() {
    LinkedHashMap<String, String> customers = new LinkedHashMap<>();
    customers.put("customer-1", "机关A");
    customers.put("customer-2", "机关B");

    Map<String, String> headers = SaleOutSheetServiceImpl.buildMarketBuySummary2Headers(customers);

    Assert.assertEquals(new ArrayList<>(headers.keySet()), Arrays.asList(
        "date", "category", "productName", "unit", "customer-customer-1",
        "customer-customer-2", "total"));
    Assert.assertEquals(new ArrayList<>(headers.values()), Arrays.asList(
        "日期", "分类", "商品名称", "单位", "机关A", "机关B", "总计"));
  }

  /**
   * 验证总重量将数量和单位拼接为“数量单位”。
   */
  @Test
  void formatTotalWithUnitShouldAppendUnitAfterQuantity() {
    Assert.assertEquals(
        SaleOutSheetMarketBuySummaryFormatter.formatTotalWithUnit(
            new BigDecimal("6.00"), "公斤"), "6公斤");
    Assert.assertEquals(
        SaleOutSheetMarketBuySummaryFormatter.formatTotalWithUnit(
            new BigDecimal("6.00"), ""), "6");
  }

  /**
   * 验证订单日期按年月日且不补零的格式输出。
   */
  @Test
  void formatOrderDateShouldUseYearMonthDayWithoutLeadingZeros() {
    Assert.assertEquals(
        SaleOutSheetMarketBuySummaryFormatter.formatOrderDate(
            LocalDate.of(2026, 7, 19)), "2026/7/19");
  }

  /**
   * 验证同一商品在不同订单日期下使用不同的汇总键。
   */
  @Test
  void buildMarketBuySummaryRowKeyShouldSeparateDifferentDates() {
    String firstKey = SaleOutSheetServiceImpl.buildMarketBuySummaryRowKey(
        LocalDate.of(2026, 7, 19), "product-1");
    String secondKey = SaleOutSheetServiceImpl.buildMarketBuySummaryRowKey(
        LocalDate.of(2026, 7, 20), "product-1");

    Assert.assertNotEquals(firstKey, secondKey);
  }

  /**
   * 验证汇总排序键按品类、商品名称、日期依次排序。
   */
  @Test
  void buildMarketBuySummarySortKeyShouldOrderCategoryProductAndDate() {
    String earlierCategory = SaleOutSheetServiceImpl.buildMarketBuySummarySortKey(
        "A类", "商品B", LocalDate.of(2026, 7, 20));
    String laterProduct = SaleOutSheetServiceImpl.buildMarketBuySummarySortKey(
        "A类", "商品C", LocalDate.of(2026, 7, 19));
    String laterCategory = SaleOutSheetServiceImpl.buildMarketBuySummarySortKey(
        "B类", "商品A", LocalDate.of(2026, 7, 1));

    Assert.assertTrue(earlierCategory.compareTo(laterProduct) < 0);
    Assert.assertTrue(laterProduct.compareTo(laterCategory) < 0);
  }

  /**
   * 创建买菜汇总测试明细。
   *
   * @param orderNum 数量
   * @param description 备注
   * @return 销售出库明细
   */
  private SaleOutSheetDetail createDetail(String orderNum, String description) {
    SaleOutSheetDetail detail = new SaleOutSheetDetail();
    detail.setSheetId("sheet-1");
    detail.setProductId("product-1");
    detail.setOrderNum(new BigDecimal(orderNum));
    detail.setDescription(description);
    return detail;
  }
}
