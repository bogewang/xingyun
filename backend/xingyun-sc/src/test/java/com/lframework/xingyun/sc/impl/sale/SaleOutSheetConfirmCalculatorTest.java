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
   * 验证每行验收金额先四舍五入到两位小数，再计算单据合计。
   */
  @Test
  void calculateAmountShouldRoundEachDetailBeforeSumming() {
    SaleOutSheet sheet = new SaleOutSheet();
    SaleOutSheetDetail first = new SaleOutSheetDetail();
    first.setConfirmNum(new BigDecimal("1.004"));
    first.setTaxPrice(BigDecimal.ONE);
    SaleOutSheetDetail second = new SaleOutSheetDetail();
    second.setConfirmNum(new BigDecimal("1.004"));
    second.setTaxPrice(BigDecimal.ONE);

    SaleOutSheetAmtCalculator.calculateSheet(sheet, Arrays.asList(first, second));

    Assert.assertEquals(first.getConfirmAmt(), new BigDecimal("1.00"));
    Assert.assertEquals(second.getConfirmAmt(), new BigDecimal("1.00"));
    Assert.assertEquals(sheet.getConfirmAmt(), new BigDecimal("2.00"));
  }

  /**
   * 验证出库数量和验收数量相等时，单行金额及单据合计金额保持一致。
   */
  @Test
  void equalQuantitiesShouldProduceEqualAmounts() {
    SaleOutSheet sheet = new SaleOutSheet();
    SaleOutSheetDetail detail = new SaleOutSheetDetail();
    detail.setBusinessNum(new BigDecimal("1.004"));
    detail.setConfirmNum(new BigDecimal("1.004"));
    detail.setTaxPrice(BigDecimal.ONE);
    detail.setTaxAmount(new BigDecimal("1.004000"));

    SaleOutSheetAmtCalculator.calculateSheet(sheet, Arrays.asList(detail));

    Assert.assertEquals(detail.getTaxAmount(), detail.getConfirmAmt());
    Assert.assertEquals(sheet.getTotalAmount(), sheet.getConfirmAmt());
  }

  /**
   * 验证空数量或空单价按 0 处理。
   */
  @Test
  void calculateAmountShouldTreatNullAsZero() {
    SaleOutSheet sheet = new SaleOutSheet();
    SaleOutSheetDetail detail = new SaleOutSheetDetail();

    SaleOutSheetAmtCalculator.calculateSheet(sheet, Arrays.asList(detail));

    Assert.assertEquals(detail.getConfirmAmt(), new BigDecimal("0.00"));
    Assert.assertEquals(sheet.getConfirmAmt(), new BigDecimal("0.00"));
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

    SaleOutSheetAmtCalculator.calculateSheet(sheet, Arrays.asList(first, second));

    Assert.assertEquals(first.getConfirmAmt(), new BigDecimal("6.50"));
    Assert.assertEquals(second.getConfirmAmt(), new BigDecimal("6.00"));
    Assert.assertEquals(sheet.getConfirmNum(), new BigDecimal("3.5"));
    Assert.assertEquals(sheet.getConfirmAmt(), new BigDecimal("12.50"));
  }
}
