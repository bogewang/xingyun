package com.lframework.xingyun.sc.impl.sale;

import com.lframework.xingyun.basedata.entity.Customer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import org.testng.Assert;
import org.testng.annotations.Test;

class SaleOutSheetMarketBuySummaryFormatterTest {

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
}
