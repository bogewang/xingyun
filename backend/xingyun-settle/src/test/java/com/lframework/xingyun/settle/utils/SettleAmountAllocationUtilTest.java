package com.lframework.xingyun.settle.utils;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * 供应商对账、结算金额分摊测试
 */
public class SettleAmountAllocationUtilTest {

  /**
   * 验证确认金额大于基数合计时，差额平均分摊到每张单据。
   */
  @Test
  public void allocate_shouldDistributePositiveDifferenceAndKeepTotal() {
    List<BigDecimal> result = SettleAmountAllocationUtil.allocate(
        new BigDecimal("2800.00"),
        Arrays.asList(new BigDecimal("930.10"), new BigDecimal("569.50"),
            new BigDecimal("1073.20")));

    Assert.assertEquals(Arrays.asList(new BigDecimal("1005.83"), new BigDecimal("645.23"),
        new BigDecimal("1148.94")), result);
    Assert.assertEquals(new BigDecimal("2800.00"), sum(result));
  }

  /**
   * 验证确认金额小于基数合计时，差额平均扣减且最后一张单据补齐尾差。
   */
  @Test
  public void allocate_shouldDistributeNegativeDifferenceAndPutRemainderOnLastItem() {
    List<BigDecimal> result = SettleAmountAllocationUtil.allocate(
        new BigDecimal("1004.91"),
        Arrays.asList(new BigDecimal("930.10"), new BigDecimal("569.50"),
            new BigDecimal("1073.20")));

    Assert.assertEquals(Arrays.asList(new BigDecimal("407.47"), new BigDecimal("46.87"),
        new BigDecimal("550.57")), result);
    Assert.assertEquals(new BigDecimal("1004.91"), sum(result));
  }

  /**
   * 验证确认金额过小时拒绝生成负数明细。
   */
  @Test(expected = DefaultClientException.class)
  public void allocate_shouldRejectNegativeDetailAmount() {
    SettleAmountAllocationUtil.allocate(new BigDecimal("20.00"),
        Arrays.asList(new BigDecimal("50.00"), new BigDecimal("100.00")));
  }

  /**
   * 验证正向销售与负向退货按净额混合分摊时保留各自方向。
   */
  @Test
  public void allocateSignedShouldKeepMixedSaleAndReturnDirections() {
    List<BigDecimal> result = SettleAmountAllocationUtil.allocateSigned(
        new BigDecimal("80.00"),
        Arrays.asList(new BigDecimal("100.00"), new BigDecimal("-20.00")));

    Assert.assertEquals(Arrays.asList(new BigDecimal("100.00"), new BigDecimal("-20.00")),
        result);
    Assert.assertEquals(new BigDecimal("80.00"), sum(result));
  }

  /**
   * 验证纯退货可按负金额执行部分退款。
   */
  @Test
  public void allocateSignedShouldSupportNegativeRefund() {
    List<BigDecimal> result = SettleAmountAllocationUtil.allocateSigned(
        new BigDecimal("-10.00"),
        Arrays.asList(new BigDecimal("-20.00")));

    Assert.assertEquals(Arrays.asList(new BigDecimal("-10.00")), result);
  }

  /**
   * 汇总分摊金额。
   *
   * @param amounts 分摊金额
   * @return 汇总金额
   */
  private BigDecimal sum(List<BigDecimal> amounts) {
    return amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
