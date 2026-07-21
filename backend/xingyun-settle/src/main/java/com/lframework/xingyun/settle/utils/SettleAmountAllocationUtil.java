package com.lframework.xingyun.settle.utils;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 供应商对账、结算金额分摊工具
 */
public final class SettleAmountAllocationUtil {

  private static final int AMOUNT_SCALE = 2;

  private SettleAmountAllocationUtil() {
  }

  /**
   * 将确认金额与基数合计的差额平均分摊到每张单据。
   *
   * @param confirmedAmount 确认总金额
   * @param baseAmounts 各单据分摊基数
   * @return 与基数列表顺序一致的分摊金额
   */
  public static List<BigDecimal> allocate(BigDecimal confirmedAmount,
      List<BigDecimal> baseAmounts) {

    if (confirmedAmount == null) {
      throw new DefaultClientException("确认金额不能为空！");
    }
    if (baseAmounts == null || baseAmounts.isEmpty()) {
      return Collections.emptyList();
    }

    BigDecimal totalAmount = normalize(confirmedAmount);
    BigDecimal baseTotalAmount = baseAmounts.stream().map(SettleAmountAllocationUtil::normalize)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal averageDifference = totalAmount.subtract(baseTotalAmount)
        .divide(BigDecimal.valueOf(baseAmounts.size()), AMOUNT_SCALE, RoundingMode.HALF_UP);

    List<BigDecimal> results = new ArrayList<>(baseAmounts.size());
    BigDecimal allocatedAmount = BigDecimal.ZERO;
    for (int index = 0; index < baseAmounts.size(); index++) {
      BigDecimal amount = index == baseAmounts.size() - 1
          ? totalAmount.subtract(allocatedAmount)
          : normalize(baseAmounts.get(index)).add(averageDifference);
      if (amount.compareTo(BigDecimal.ZERO) < 0) {
        throw new DefaultClientException("确认金额过小，分摊后会出现负数单据，请调整确认金额！");
      }
      results.add(amount);
      allocatedAmount = allocatedAmount.add(amount);
    }
    return results;
  }

  /**
   * 规范金额精度。
   *
   * @param amount 原始金额
   * @return 两位小数金额
   */
  private static BigDecimal normalize(BigDecimal amount) {
    return (amount == null ? BigDecimal.ZERO : amount).setScale(AMOUNT_SCALE,
        RoundingMode.HALF_UP);
  }
}
