package com.lframework.xingyun.sc.impl.sale;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;
import java.util.Collections;
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
}
