package com.lframework.xingyun.sc.impl.purchase;

import com.lframework.xingyun.sc.excel.purchase.receive.ReceiveSheetImportModel;
import com.lframework.xingyun.sc.excel.purchase.receive.ReceiveSheetQueryImportModel;
import java.math.BigDecimal;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

class ReceiveSheetServiceImplTest {

  @Test
  void normalizeQueryImportNumbersShouldConvertNullQuantityToZero() {
    ReceiveSheetQueryImportModel model = new ReceiveSheetQueryImportModel();
    model.setReceiveNum(null);

    ReceiveSheetServiceImpl.normalizeQueryImportNumbers(model);

    Assert.assertEquals(model.getReceiveNum(), BigDecimal.ZERO);
  }

  @Test
  void validateImportNumbersShouldAllowNullValues() {
    ReceiveSheetImportModel model = createModel(null, null);

    Assert.assertTrue(ReceiveSheetServiceImpl.validateImportNumbers(model).isEmpty());
  }

  @Test
  void validateImportNumbersShouldAllowZeroValues() {
    ReceiveSheetImportModel model = createModel(BigDecimal.ZERO, BigDecimal.ZERO);

    Assert.assertTrue(ReceiveSheetServiceImpl.validateImportNumbers(model).isEmpty());
  }

  @Test
  void validateImportNumbersShouldRejectNegativeValues() {
    ReceiveSheetImportModel model = createModel(new BigDecimal("-0.00000001"), new BigDecimal("-0.000001"));

    List<String> errors = ReceiveSheetServiceImpl.validateImportNumbers(model);

    Assert.assertTrue(errors.contains("第2行“数量”不允许小于0"));
    Assert.assertTrue(errors.contains("第2行“单价”不允许小于0"));
  }

  @Test
  void validateImportNumbersShouldRetainPrecisionLimits() {
    ReceiveSheetImportModel model = createModel(new BigDecimal("1.123456789"), new BigDecimal("1.1234567"));

    List<String> errors = ReceiveSheetServiceImpl.validateImportNumbers(model);

    Assert.assertTrue(errors.contains("第2行“数量”最多允许8位小数"));
    Assert.assertTrue(errors.contains("第2行“单价”最多允许6位小数"));
  }

  @Test
  void validateProductionDateShouldRequireStrictCalendarDate() {
    Assert.assertTrue(ReceiveSheetServiceImpl.validateProductionDate(null, "第2行").isEmpty());
    Assert.assertTrue(
        ReceiveSheetServiceImpl.validateProductionDate("2024.02.29", "第2行").isEmpty());
    Assert.assertEquals(
        ReceiveSheetServiceImpl.validateProductionDate("2026.02.30", "第2行").get(0),
        "第2行商品生产日期格式错误，应为yyyy.MM.dd且必须是有效日期");
    Assert.assertEquals(
        ReceiveSheetServiceImpl.validateProductionDate("2026-07-16", "第2行").get(0),
        "第2行商品生产日期格式错误，应为yyyy.MM.dd且必须是有效日期");
  }

  private ReceiveSheetImportModel createModel(BigDecimal receiveNum, BigDecimal purchasePrice) {
    ReceiveSheetImportModel model = new ReceiveSheetImportModel();
    model.setSeq(2);
    model.setReceiveNum(receiveNum);
    model.setPurchasePrice(purchasePrice);
    return model;
  }
}
