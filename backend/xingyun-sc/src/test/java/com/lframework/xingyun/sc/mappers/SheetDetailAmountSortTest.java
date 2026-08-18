package com.lframework.xingyun.sc.mappers;

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

  /**
   * 验证采购入库和销售出库明细均允许按明细金额排序。
   */
  @Test
  void shouldSupportTaxAmountSortForSheetDetails() throws NoSuchMethodException {
    assertTaxAmountSort(ReceiveSheetMapper.class);
    assertTaxAmountSort(SaleOutSheetMapper.class);
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
}
