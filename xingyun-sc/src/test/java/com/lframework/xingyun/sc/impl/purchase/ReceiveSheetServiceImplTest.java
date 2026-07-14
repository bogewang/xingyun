package com.lframework.xingyun.sc.impl.purchase;

import com.lframework.xingyun.sc.excel.purchase.receive.ReceiveSheetImportModel;
import java.math.BigDecimal;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

class ReceiveSheetServiceImplTest {

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

  private ReceiveSheetImportModel createModel(BigDecimal receiveNum, BigDecimal purchasePrice) {
    ReceiveSheetImportModel model = new ReceiveSheetImportModel();
    model.setSeq(2);
    model.setReceiveNum(receiveNum);
    model.setPurchasePrice(purchasePrice);
    return model;
  }
}
