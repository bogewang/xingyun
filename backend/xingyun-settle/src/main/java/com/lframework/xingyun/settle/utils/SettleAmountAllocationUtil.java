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
   * 按净额比例分摊包含负向退款的确认金额。
   *
   * @param confirmedAmount 确认净额
   * @param baseAmounts 各单据带方向的未结算金额
   * @return 与基数列表顺序一致且保留方向的分摊金额
   */
  public static List<BigDecimal> allocateSigned(BigDecimal confirmedAmount,
      List<BigDecimal> baseAmounts) {
    if (confirmedAmount == null) {
      throw new DefaultClientException("确认金额不能为空！");
    }
    if (baseAmounts == null || baseAmounts.isEmpty()) {
      return Collections.emptyList();
    }
    boolean containsNegative = baseAmounts.stream().map(SettleAmountAllocationUtil::normalize)
        .anyMatch(amount -> amount.compareTo(BigDecimal.ZERO) < 0);
    if (!containsNegative) {
      return allocate(confirmedAmount, baseAmounts);
    }

    BigDecimal totalAmount = normalize(confirmedAmount);
    BigDecimal baseTotalAmount = baseAmounts.stream().map(SettleAmountAllocationUtil::normalize)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (totalAmount.compareTo(BigDecimal.ZERO) == 0
        || baseTotalAmount.compareTo(BigDecimal.ZERO) == 0
        || totalAmount.signum() != baseTotalAmount.signum()
        || totalAmount.abs().compareTo(baseTotalAmount.abs()) > 0) {
      throw new DefaultClientException("确认金额与所选单据净额方向或范围不一致！");
    }

    BigDecimal ratio = totalAmount.divide(baseTotalAmount, 12, RoundingMode.HALF_UP);
    int remainderIndex = -1;
    for (int index = baseAmounts.size() - 1; index >= 0; index--) {
      if (normalize(baseAmounts.get(index)).compareTo(BigDecimal.ZERO) != 0) {
        remainderIndex = index;
        break;
      }
    }
    List<BigDecimal> results = new ArrayList<>(Collections.nCopies(baseAmounts.size(),
        BigDecimal.ZERO.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP)));
    BigDecimal allocatedAmount = BigDecimal.ZERO;
    for (int index = 0; index < baseAmounts.size(); index++) {
      if (index == remainderIndex) {
        continue;
      }
      BigDecimal baseAmount = normalize(baseAmounts.get(index));
      BigDecimal amount = baseAmount.multiply(ratio).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
      validateSignedAllocation(baseAmount, amount);
      results.set(index, amount);
      allocatedAmount = allocatedAmount.add(amount);
    }
    BigDecimal remainder = totalAmount.subtract(allocatedAmount);
    BigDecimal remainderBase = normalize(baseAmounts.get(remainderIndex));
    validateSignedAllocation(remainderBase, remainder);
    results.set(remainderIndex, remainder);
    return results;
  }

  /**
   * 校验分摊结果未改变业务方向且未超过单据未结算额。
   *
   * @param baseAmount 带方向的未结算额
   * @param allocatedAmount 分摊金额
   */
  private static void validateSignedAllocation(BigDecimal baseAmount,
      BigDecimal allocatedAmount) {
    if (allocatedAmount.compareTo(BigDecimal.ZERO) != 0
        && allocatedAmount.signum() != baseAmount.signum()) {
      throw new DefaultClientException("分摊金额不能改变业务单据的收退款方向！");
    }
    if (allocatedAmount.abs().compareTo(baseAmount.abs()) > 0) {
      throw new DefaultClientException("分摊金额不能超过业务单据的未结算金额！");
    }
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
