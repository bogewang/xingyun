package com.lframework.xingyun.sc.mappers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class SaleOutSheetProfitTrendSqlTest {

  /** 验证销售趋势毛利率的分母优先汇总验收金额。 */
  @Test
  void shouldPreferConfirmAmountWhenCalculatingProfitRate() throws IOException {
    String mapperXml = readMapperXml();
    String querySql = getQuerySql(mapperXml, "queryProfitTrend");

    assertTrue(querySql.contains("SUM(CASE WHEN IFNULL(d.confirm_amt, 0) != 0"));
  }

  /** 验证总览和趋势图销售金额均优先汇总验收金额。 */
  @Test
  void shouldUseConfirmAmountAsSummaryAndTrendSalesAmount() throws IOException {
    String mapperXml = readMapperXml();
    String summarySql = getQuerySql(mapperXml, "queryProfitSummary");
    String trendSql = getQuerySql(mapperXml, "queryProfitTrend");

    assertTrue(summarySql.contains(
        "SUM(CASE WHEN IFNULL(s.confirm_amt, 0) != 0 THEN s.confirm_amt ELSE s.total_amount END)"));
    assertTrue(trendSql.contains(
        "SUM(CASE WHEN IFNULL(d.confirm_amt, 0) != 0 THEN d.confirm_amt ELSE d.tax_amount END), 0) AS salesAmount"));
  }

  /** 验证商品利润报表使用验收金额反推销售成本，避免正成本显示为负数。 */
  @Test
  void shouldUseConfirmAmountWhenCalculatingProductProfitCost() throws IOException {
    String mapperXml = readMapperXml();
    String productSql = getQuerySql(mapperXml, "queryProductProfit");

    assertTrue(productSql.contains(
        "SUM(CASE WHEN IFNULL(d.confirm_amt, 0) != 0 THEN d.confirm_amt ELSE d.tax_amount END\n"
            + "                - IFNULL(d.total_profit, 0))"));
  }

  /** 验证结算状态和未付金额筛选统一按验收金额计算应收金额。 */
  @Test
  void shouldUseConfirmAmountForSettlementAndUnpaidFilters() throws IOException {
    String mapperXml = readMapperXml();

    assertTrue(mapperXml.contains(
        "(CASE WHEN IFNULL(s.confirm_amt, 0) != 0 THEN s.confirm_amt ELSE s.total_amount END) &lt;= IFNULL(s.paid_amount, 0)"));
    assertTrue(mapperXml.contains(
        "(CASE WHEN IFNULL(s.confirm_amt, 0) != 0 THEN s.confirm_amt ELSE s.total_amount END) &gt; IFNULL(s.paid_amount, 0)"));
    assertTrue(mapperXml.contains(
        "(CASE WHEN IFNULL(s1.confirm_amt, 0) != 0 THEN s1.confirm_amt ELSE s1.total_amount END) &lt;= IFNULL(s1.paid_amount, 0)"));
    assertTrue(mapperXml.contains(
        "((CASE WHEN IFNULL(s.confirm_amt, 0) != 0 THEN s.confirm_amt ELSE s.total_amount END) - IFNULL(s.paid_amount, 0)) &gt;= #{vo.unpaidAmountStart}"));
    assertFalse(mapperXml.contains(
        "AND IFNULL(s.total_amount, 0) &lt;= IFNULL(s.paid_amount, 0)"));
    assertFalse(mapperXml.contains(
        "AND IFNULL(s.total_amount, 0) &gt; IFNULL(s.paid_amount, 0)"));
    assertFalse(mapperXml.contains(
        "AND IFNULL(s1.total_amount, 0) &lt;= IFNULL(s1.paid_amount, 0)"));
    assertFalse(mapperXml.contains(
        "AND IFNULL(s1.total_amount, 0) &gt; IFNULL(s1.paid_amount, 0)"));
  }

  /** 读取 Mapper 文件内容。 */
  private String readMapperXml() throws IOException {
    return new String(Files.readAllBytes(
        Paths.get("src/main/resources/mappers/sale/SaleOutSheetMapper.xml")), StandardCharsets.UTF_8);
  }

  /** 截取指定查询的 SQL 内容。 */
  private String getQuerySql(String mapperXml, String queryId) {
    int queryStart = mapperXml.indexOf("<select id=\"" + queryId + "\"");
    int queryEnd = mapperXml.indexOf("</select>", queryStart);
    return mapperXml.substring(queryStart, queryEnd);
  }
}
