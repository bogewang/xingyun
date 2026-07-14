package com.lframework.xingyun.sc.impl.product;

import java.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 仓库业务商品引用检查器测试。
 */
class ScProductReferenceCheckerTest {

  /**
   * 验证任一引用计数大于零时判定商品已被引用。
   */
  @Test
  void shouldReturnTrueWhenAnyReferenceExists() {
    Assert.assertTrue(ScProductReferenceChecker.hasReference(
        Arrays.asList(0L, 0L, 1L, 0L, 0L)));
  }

  /**
   * 验证全部引用计数为零时判定商品未被引用。
   */
  @Test
  void shouldReturnFalseWhenNoReferenceExists() {
    Assert.assertFalse(ScProductReferenceChecker.hasReference(
        Arrays.asList(0L, 0L, 0L, 0L, 0L)));
  }
}
