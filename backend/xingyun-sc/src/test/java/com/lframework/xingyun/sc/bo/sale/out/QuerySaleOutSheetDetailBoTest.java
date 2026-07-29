package com.lframework.xingyun.sc.bo.sale.out;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lframework.xingyun.sc.dto.sale.out.QuerySaleOutSheetDetailDto;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class QuerySaleOutSheetDetailBoTest {

  /** 验证明细接口返回验收数量和验收金额。 */
  @Test
  void shouldExposeConfirmFields() {
    QuerySaleOutSheetDetailDto dto = new QuerySaleOutSheetDetailDto();
    dto.setConfirmNum(new BigDecimal("2.5"));
    dto.setConfirmAmt(new BigDecimal("38.05"));

    QuerySaleOutSheetDetailBo result = new QuerySaleOutSheetDetailBo(dto);

    assertEquals(new BigDecimal("2.5"), result.getConfirmNum());
    assertEquals(new BigDecimal("38.05"), result.getConfirmAmt());
  }
}
