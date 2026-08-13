package com.lframework.xingyun.sc.impl.sale;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetImportModel;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetQueryImportModel;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
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

  /**
   * 验证刷新成本时优先使用验收数量计算成本金额。
   */
  @Test
  void resolveCostNumShouldPreferPositiveConfirmNum() {
    SaleOutSheetDetail detail = new SaleOutSheetDetail();
    detail.setOrderNum(new BigDecimal("10"));
    detail.setConfirmNum(new BigDecimal("6.5"));

    Assert.assertEquals(SaleOutSheetServiceImpl.resolveCostNum(detail), new BigDecimal("6.5"));
  }

  /**
   * 验证刷新成本时验收数量为空才使用出库数量计算成本金额。
   */
  @Test
  void resolveCostNumShouldFallbackToOrderNumWhenConfirmNumNull() {
    SaleOutSheetDetail detail = new SaleOutSheetDetail();
    detail.setOrderNum(new BigDecimal("10"));

    Assert.assertEquals(SaleOutSheetServiceImpl.resolveCostNum(detail), new BigDecimal("10"));
  }

  /**
   * 验证刷新成本时验收数量为 0 也按验收数量计算成本金额。
   */
  @Test
  void resolveCostNumShouldUseZeroConfirmNum() {
    SaleOutSheetDetail detail = new SaleOutSheetDetail();
    detail.setOrderNum(new BigDecimal("10"));
    detail.setConfirmNum(BigDecimal.ZERO);

    Assert.assertEquals(SaleOutSheetServiceImpl.resolveCostNum(detail), BigDecimal.ZERO);
  }

  /**
   * 验证合并单据ID会去除空值并去重。
   */
  @Test
  void normalizeMergeSheetIdsShouldRemoveBlankAndDuplicateIds() {
    List<String> ids = SaleOutSheetServiceImpl.normalizeMergeSheetIds(
        Arrays.asList("sale-1", "", "sale-2", "sale-1", null));

    Assert.assertEquals(ids, Arrays.asList("sale-1", "sale-2"));
  }

  /**
   * 验证合并单据至少需要两张有效单据。
   */
  @Test(expectedExceptions = DefaultClientException.class)
  void normalizeMergeSheetIdsShouldRejectLessThanTwoIds() {
    SaleOutSheetServiceImpl.normalizeMergeSheetIds(Arrays.asList("sale-1", "", "sale-1"));
  }

  /**
   * 验证合并时保留创建时间最早的单据。
   */
  @Test
  void sortMergeSheetsShouldPutEarliestCreateTimeFirst() {
    SaleOutSheet lateSheet = createSheet("sale-2", "SO-002",
        LocalDateTime.of(2026, 8, 12, 10, 0));
    SaleOutSheet earlySheet = createSheet("sale-1", "SO-001",
        LocalDateTime.of(2026, 8, 12, 9, 0));

    List<SaleOutSheet> sheets = Arrays.asList(lateSheet, earlySheet);

    SaleOutSheetServiceImpl.sortMergeSheets(sheets);

    Assert.assertEquals(sheets.get(0).getId(), "sale-1");
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

  private SaleOutSheet createSheet(String id, String code, LocalDateTime createTime) {
    SaleOutSheet sheet = new SaleOutSheet();
    sheet.setId(id);
    sheet.setCode(code);
    sheet.setCreateTime(createTime);
    return sheet;
  }
}
