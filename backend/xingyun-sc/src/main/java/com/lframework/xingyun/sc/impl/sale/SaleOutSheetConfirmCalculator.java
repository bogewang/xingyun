package com.lframework.xingyun.sc.impl.sale;

import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 销售出库单验收金额计算器。
 */
public final class SaleOutSheetConfirmCalculator {

  private static final BigDecimal SIX_DECIMAL_ZERO = BigDecimal.ZERO.setScale(6);

  private SaleOutSheetConfirmCalculator() {
  }

  /**
   * 计算验收金额。
   *
   * @param confirmNum 验收数量
   * @param taxPrice 含税单价
   * @return 验收金额，固定保留 6 位小数
   */
  public static BigDecimal calculateAmount(BigDecimal confirmNum, BigDecimal taxPrice) {
    if (confirmNum == null || taxPrice == null) {
      return SIX_DECIMAL_ZERO;
    }

    return confirmNum.multiply(taxPrice).setScale(6, RoundingMode.HALF_UP);
  }

  /**
   * 计算单据明细的验收金额，并规范化验收数量。
   *
   * @param detail 销售出库单明细
   */
  public static void calculateDetail(SaleOutSheetDetail detail) {
    if (detail == null) {
      return;
    }

    BigDecimal confirmNum = normalizeAmount(detail.getConfirmNum());
    detail.setConfirmNum(confirmNum);
    detail.setConfirmAmt(calculateAmount(confirmNum, detail.getTaxPrice()));
  }

  /**
   * 计算单据头部验收汇总，并覆盖原有头部值。
   *
   * @param sheet 销售出库单
   * @param details 销售出库单明细
   */
  public static void calculateSheet(SaleOutSheet sheet, List<SaleOutSheetDetail> details) {
    if (sheet == null) {
      return;
    }

    BigDecimal totalConfirmNum = SIX_DECIMAL_ZERO;
    BigDecimal totalConfirmAmt = SIX_DECIMAL_ZERO;
    if (details != null) {
      for (SaleOutSheetDetail detail : details) {
        if (detail == null) {
          continue;
        }
        calculateDetail(detail);
        totalConfirmNum = totalConfirmNum.add(detail.getConfirmNum() == null ? SIX_DECIMAL_ZERO
            : detail.getConfirmNum());
        totalConfirmAmt = totalConfirmAmt.add(detail.getConfirmAmt() == null ? SIX_DECIMAL_ZERO
            : detail.getConfirmAmt());
      }
    }

    sheet.setConfirmNum(totalConfirmNum.setScale(6, RoundingMode.HALF_UP));
    sheet.setConfirmAmt(totalConfirmAmt.setScale(6, RoundingMode.HALF_UP));
  }

  /**
   * 将数量规范化为 6 位小数。
   *
   * @param amount 原始数量
   * @return 规范化后的数量
   */
  private static BigDecimal normalizeAmount(BigDecimal amount) {
    if (amount == null) {
      return SIX_DECIMAL_ZERO;
    }

    return amount.setScale(6, RoundingMode.HALF_UP);
  }
}
