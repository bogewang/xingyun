package com.lframework.xingyun.basedata.impl.quote;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheet;
import com.lframework.xingyun.basedata.enums.quote.QuoteSheetStatus;
import com.lframework.xingyun.basedata.vo.quote.QuoteSheetProductVo;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.testng.Assert;
import org.testng.annotations.Test;

/** 报价单服务规则测试。 */
public class QuoteSheetServiceImplTest {
  /** 相邻闭区间不重叠。 */
  @Test public void shouldTreatAdjacentDateRangesAsNonOverlapping() { Assert.assertFalse(QuoteSheetServiceImpl.isDateRangeOverlapped(date("2026-08-01"),date("2026-08-31"),date("2026-09-01"),date("2026-09-30"))); }
  /** 端点相同的闭区间应重叠。 */
  @Test public void shouldTreatSharedEndpointAsOverlapping() { Assert.assertTrue(QuoteSheetServiceImpl.isDateRangeOverlapped(date("2026-08-01"),date("2026-08-31"),date("2026-08-31"),date("2026-09-30"))); }
  /** 重叠定价周期应拒绝。 */
  @Test(expectedExceptions=DefaultClientException.class,expectedExceptionsMessageRegExp=".*定价周期.*冲突.*") public void shouldRejectOverlappedQuotePeriod() { QuoteSheetServiceImpl.assertNoDateRangeOverlap("quote-2",date("2026-08-01"),date("2026-08-31"),Collections.singletonList(sheet("quote-1","2026-08-31","2026-09-30"))); }
  /** 排除自身后允许更新原报价周期。 */
  @Test public void shouldExcludeCurrentQuoteSheetFromOverlapCheck() { QuoteSheetServiceImpl.assertNoDateRangeOverlap("quote-1",date("2026-08-01"),date("2026-08-31"),Collections.singletonList(sheet("quote-1","2026-08-01","2026-08-31"))); }
  /** 同单重复商品应拒绝。 */
  @Test(expectedExceptions=DefaultClientException.class,expectedExceptionsMessageRegExp=".*商品不能重复.*") public void shouldRejectDuplicatedProducts() { QuoteSheetProductVo first=new QuoteSheetProductVo(); first.setProductId("product-1"); QuoteSheetProductVo second=new QuoteSheetProductVo(); second.setProductId("product-1"); QuoteSheetServiceImpl.assertNoDuplicatedProducts(Arrays.asList(first,second)); }
  /** 停用报价单不参与生效报价查询。 */
  @Test public void shouldNotTreatDisabledQuoteAsActive() { QuoteSheet disabled=sheet("quote-1","2026-08-01","2026-08-31"); disabled.setStatus(QuoteSheetStatus.DISABLED); Assert.assertFalse(QuoteSheetServiceImpl.isActiveOn(disabled,date("2026-08-15"))); }
  /** 服务层保存时空商品明细应被拒绝。 */
  @Test(expectedExceptions=DefaultClientException.class,expectedExceptionsMessageRegExp=".*商品明细不能为空.*") public void shouldRejectEmptyProductsInServiceLayer() { QuoteSheetServiceImpl.assertBasicSheetData(date("2026-08-01"),date("2026-08-31"),Collections.emptyList()); }
  /** 启用前非法日期应被共用校验拒绝。 */
  @Test(expectedExceptions=DefaultClientException.class,expectedExceptionsMessageRegExp=".*开始日期不能晚于.*") public void shouldRejectInvalidDatesBeforeEnabling() { QuoteSheetServiceImpl.assertBasicSheetData(date("2026-08-31"),date("2026-08-01"),Collections.singletonList("product-1")); }
  /** 有效报价 SQL 必须排除停用商品。 */
  @Test public void shouldExcludeDisabledProductsFromActiveQuoteQuery() throws Exception { String sql=new String(Files.readAllBytes(Paths.get("src/main/resources/mappers/quote/QuoteSheetMapper.xml")),StandardCharsets.UTF_8); Assert.assertTrue(sql.contains("p.available = TRUE")); }
  /** 构造报价单。 */
  private QuoteSheet sheet(String id,String start,String end) { QuoteSheet sheet=new QuoteSheet(); sheet.setId(id); sheet.setStartDate(date(start)); sheet.setEndDate(date(end)); return sheet; }
  /** 解析测试日期。 */
  private LocalDate date(String value) { return LocalDate.parse(value); }
}
