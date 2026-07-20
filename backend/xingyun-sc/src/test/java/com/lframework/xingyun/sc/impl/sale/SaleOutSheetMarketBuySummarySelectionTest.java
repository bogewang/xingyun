package com.lframework.xingyun.sc.impl.sale;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.Test;

class SaleOutSheetMarketBuySummarySelectionTest {

  /**
   * 验证买菜汇总必须传入勾选的单据ID。
   */
  @Test
  void validateMarketBuySummaryIdsShouldRejectEmptyIds() {
    QuerySaleOutSheetVo vo = new QuerySaleOutSheetVo();
    vo.setIdList(Collections.emptyList());

    try {
      SaleOutSheetServiceImpl.validateMarketBuySummaryIds(vo);
      Assert.fail("空单据ID列表应该抛出业务异常");
    } catch (DefaultClientException e) {
      Assert.assertEquals(e.getMessage(), "请选择要汇总的销售出库单！");
    }
  }

  /**
   * 验证买菜汇总2入口会在访问依赖前校验勾选的单据ID。
   */
  @Test
  void marketBuySummary2ShouldRejectEmptyIdsBeforeAccessingDependencies() {
    QuerySaleOutSheetVo vo = new QuerySaleOutSheetVo();
    vo.setIdList(Collections.emptyList());

    try {
      new SaleOutSheetServiceImpl().marketBuySummary2(vo);
      Assert.fail("空单据ID列表应该抛出业务异常");
    } catch (DefaultClientException e) {
      Assert.assertEquals(e.getMessage(), "请选择要汇总的销售出库单！");
    }
  }

  /**
   * 验证买菜汇总2按勾选单据顺序重排逆序的数据库查询结果。
   */
  @Test
  void sortMarketBuySummary2SheetsBySelectionShouldFollowSelectedOrder() {
    SaleOutSheet sheetB = createSheet("sheet-B", "customer-B");
    SaleOutSheet sheetA = createSheet("sheet-A", "customer-A");

    List<SaleOutSheet> sortedSheets = SaleOutSheetServiceImpl.sortMarketBuySummary2SheetsBySelection(
        Arrays.asList(sheetB, sheetA), Arrays.asList("sheet-A", "sheet-B"));

    Assert.assertEquals(sortedSheets.stream().map(SaleOutSheet::getId).collect(Collectors.toList()),
        Arrays.asList("sheet-A", "sheet-B"));
  }

  /**
   * 验证重排后的单据让重复客户保留首次出现位置。
   */
  @Test
  void sortMarketBuySummary2SheetsBySelectionShouldKeepDuplicateCustomerFirstPosition() {
    SaleOutSheet sheetB = createSheet("sheet-B", "customer-B");
    SaleOutSheet sheetC = createSheet("sheet-C", "customer-A");
    SaleOutSheet sheetA = createSheet("sheet-A", "customer-A");

    List<SaleOutSheet> sortedSheets = SaleOutSheetServiceImpl.sortMarketBuySummary2SheetsBySelection(
        Arrays.asList(sheetB, sheetC, sheetA), Arrays.asList("sheet-A", "sheet-B", "sheet-C"));

    Assert.assertEquals(sortedSheets.stream().map(SaleOutSheet::getCustomerId).distinct()
        .collect(Collectors.toList()), Arrays.asList("customer-A", "customer-B"));
  }

  /**
   * 创建用于买菜汇总2排序测试的销售出库单。
   *
   * @param id 单据ID
   * @param customerId 客户ID
   * @return 销售出库单
   */
  private SaleOutSheet createSheet(String id, String customerId) {
    SaleOutSheet sheet = new SaleOutSheet();
    sheet.setId(id);
    sheet.setCustomerId(customerId);
    return sheet;
  }
}
