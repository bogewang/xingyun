package com.lframework.xingyun.sc.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lframework.starter.web.core.annotations.sort.Sort;
import com.lframework.starter.web.core.annotations.sort.Sorts;
import com.lframework.xingyun.sc.vo.purchase.receive.QueryReceiveSheetVo;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * 入库和出库明细金额排序配置测试。
 */
class SheetDetailAmountSortTest {

  /** 验证采购入库和销售出库明细均允许按明细金额排序。 */
  @Test
  void shouldSupportTaxAmountSortForSheetDetails() throws NoSuchMethodException {
    assertTaxAmountSort(ReceiveSheetMapper.class);
    assertTaxAmountSort(SaleOutSheetMapper.class);
  }

  /** 验证销售出库明细的业务字段均在排序白名单中。 */
  @Test
  void shouldSupportRequestedSaleOutDetailSortFields() throws NoSuchMethodException {
    Method method = SaleOutSheetMapper.class.getMethod("queryDetail", QuerySaleOutSheetVo.class);
    Sorts sorts = method.getAnnotation(Sorts.class);
    assertTrue(sorts != null);

    assertSort(sorts, "orderNum", "d", true);
    assertSort(sorts, "taxPrice", "d", true);
    assertSort(sorts, "costPrice", "", false);
    assertSort(sorts, "confirmNum", "d", true);
    assertSort(sorts, "confirmAmt", "d", true);
    assertSort(sorts, "costAmount", "", false);
    assertSort(sorts, "totalProfit", "d", true);
    assertSort(sorts, "profitRate", "", false);
    assertSort(sorts, "description", "d", true);
  }

  /**
   * 验证指定 Mapper 的明细查询排序白名单包含明细金额字段。
   *
   * @param mapperClass Mapper 类型
   */
  private void assertTaxAmountSort(Class<?> mapperClass) throws NoSuchMethodException {
    Method method = mapperClass.getMethod("queryDetail", getQueryVoType(mapperClass));
    Sorts sorts = method.getAnnotation(Sorts.class);
    assertTrue(sorts != null);

    for (Sort sort : sorts.value()) {
      if ("taxAmount".equals(sort.value())) {
        assertTrue("d".equals(sort.alias()));
        assertTrue(sort.autoParse());
        return;
      }
    }

    throw new AssertionError("明细查询未配置 taxAmount 排序");
  }

  /**
   * 获取 Mapper 明细查询对应的查询参数类型。
   *
   * @param mapperClass Mapper 类型
   * @return 查询参数类型
   */
  private Class<?> getQueryVoType(Class<?> mapperClass) {
    if (ReceiveSheetMapper.class.equals(mapperClass)) {
      return QueryReceiveSheetVo.class;
    }
    return QuerySaleOutSheetVo.class;
  }

  /**
   * 验证排序字段的别名及字段名解析配置。
   *
   * @param sorts 排序白名单
   * @param value 字段名
   * @param alias 表别名
   * @param autoParse 是否自动转下划线字段名
   */
  private void assertSort(Sorts sorts, String value, String alias, boolean autoParse) {
    for (Sort sort : sorts.value()) {
      if (value.equals(sort.value())) {
        assertEquals(alias, sort.alias());
        assertEquals(autoParse, sort.autoParse());
        return;
      }
    }

    throw new AssertionError("明细查询未配置 " + value + " 排序");
  }
}
