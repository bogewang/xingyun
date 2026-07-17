package com.lframework.xingyun.sc.impl.sale;

import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import java.math.BigDecimal;
import java.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 销售出库验收计算器测试。
 */
class SaleOutSheetConfirmCalculatorTest {

  /**
   * 验证验收金额按验收数量和含税单价计算，并保留 6 位小数。
   */
  @Test
  void calculateAmountShouldUseConfirmNumAndTaxPriceWithSixDecimals() {
    BigDecimal result = SaleOutSheetConfirmCalculator.calculateAmount(
        new BigDecimal("1.234567"), new BigDecimal("2.345678"));

    Assert.assertEquals(result, new BigDecimal("2.895897"));
  }

  /**
   * 验证空数量或空单价按 0 处理。
   */
  @Test
  void calculateAmountShouldTreatNullAsZero() {
    Assert.assertEquals(
        SaleOutSheetConfirmCalculator.calculateAmount(null, new BigDecimal("2")),
        BigDecimal.ZERO.setScale(6));
    Assert.assertEquals(
        SaleOutSheetConfirmCalculator.calculateAmount(new BigDecimal("2"), null),
        BigDecimal.ZERO.setScale(6));
  }

  /**
   * 验证汇总头部时按明细验收数量计算，并忽略旧的头部值。
   */
  @Test
  void calculateSheetShouldSumDetailConfirmValuesAndIgnoreExistingHeaderValues() {
    SaleOutSheet sheet = new SaleOutSheet();
    sheet.setConfirmNum(new BigDecimal("99"));
    sheet.setConfirmAmt(new BigDecimal("999"));

    SaleOutSheetDetail first = new SaleOutSheetDetail();
    first.setConfirmNum(new BigDecimal("2"));
    first.setTaxPrice(new BigDecimal("3.25"));
    SaleOutSheetDetail second = new SaleOutSheetDetail();
    second.setConfirmNum(new BigDecimal("1.5"));
    second.setTaxPrice(new BigDecimal("4"));

    SaleOutSheetConfirmCalculator.calculateSheet(sheet, Arrays.asList(first, second));

    Assert.assertEquals(first.getConfirmAmt(), new BigDecimal("6.500000"));
    Assert.assertEquals(second.getConfirmAmt(), new BigDecimal("6.000000"));
    Assert.assertEquals(sheet.getConfirmNum(), new BigDecimal("3.500000"));
    Assert.assertEquals(sheet.getConfirmAmt(), new BigDecimal("12.500000"));
  }
}
