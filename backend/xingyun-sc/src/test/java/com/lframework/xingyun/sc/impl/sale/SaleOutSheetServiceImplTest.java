package com.lframework.xingyun.sc.impl.sale;

import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetImportModel;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetQueryImportModel;
import java.math.BigDecimal;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

class SaleOutSheetServiceImplTest {

  @Test
  void normalizeQueryImportNumbersShouldConvertNullQuantityToZero() {
    SaleOutSheetQueryImportModel model = new SaleOutSheetQueryImportModel();
    model.setOrderNum(null);
    model.setConfirmNum(null);

    SaleOutSheetServiceImpl.normalizeQueryImportNumbers(model);

    Assert.assertEquals(model.getOrderNum(), BigDecimal.ZERO);
    Assert.assertEquals(model.getConfirmNum(), BigDecimal.ZERO);
  }

  @Test
  void validateImportNumbersShouldAllowNullValues() {
    SaleOutSheetImportModel model = createModel(null, null, null);

    Assert.assertTrue(SaleOutSheetServiceImpl.validateImportNumbers(model).isEmpty());
  }

  @Test
  void validateImportNumbersShouldAllowZeroValues() {
    SaleOutSheetImportModel model = createModel(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    Assert.assertTrue(SaleOutSheetServiceImpl.validateImportNumbers(model).isEmpty());
  }

  @Test
  void validateImportNumbersShouldRejectNegativeValues() {
    SaleOutSheetImportModel model = createModel(new BigDecimal("-0.00000001"), new BigDecimal("-0.000001"),
        BigDecimal.ZERO);

    List<String> errors = SaleOutSheetServiceImpl.validateImportNumbers(model);

    Assert.assertTrue(errors.contains("第2行“数量”不允许小于0"));
    Assert.assertTrue(errors.contains("第2行“单价”不允许小于0"));
  }

  @Test
  void validateImportNumbersShouldRetainPrecisionLimits() {
    SaleOutSheetImportModel model = createModel(new BigDecimal("1.123456789"), new BigDecimal("1.1234567"),
        BigDecimal.ZERO);

    List<String> errors = SaleOutSheetServiceImpl.validateImportNumbers(model);

    Assert.assertTrue(errors.contains("第2行“数量”最多允许8位小数"));
    Assert.assertTrue(errors.contains("第2行“单价”最多允许6位小数"));
  }

  @Test
  void validateImportNumbersShouldRejectNegativeConfirmNum() {
    SaleOutSheetImportModel model = createModel(BigDecimal.ONE, BigDecimal.ONE,
        new BigDecimal("-0.000001"));

    List<String> errors = SaleOutSheetServiceImpl.validateImportNumbers(model);

    Assert.assertTrue(errors.contains("第2行“验收数量”不允许小于0"));
  }

  @Test
  void validateImportNumbersShouldRejectConfirmNumWithMoreThanSixDecimals() {
    SaleOutSheetImportModel model = createModel(BigDecimal.ONE, BigDecimal.ONE,
        new BigDecimal("1.1234567"));

    List<String> errors = SaleOutSheetServiceImpl.validateImportNumbers(model);

    Assert.assertTrue(errors.contains("第2行“验收数量”最多允许6位小数"));
  }

  private SaleOutSheetImportModel createModel(BigDecimal orderNum, BigDecimal taxPrice,
      BigDecimal confirmNum) {
    SaleOutSheetImportModel model = new SaleOutSheetImportModel();
    model.setSeq(2);
    model.setOrderNum(orderNum);
    model.setTaxPrice(taxPrice);
    model.setConfirmNum(confirmNum);
    return model;
  }
}
