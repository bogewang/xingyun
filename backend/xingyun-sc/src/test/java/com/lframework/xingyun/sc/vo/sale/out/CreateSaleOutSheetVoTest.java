package com.lframework.xingyun.sc.vo.sale.out;

import com.lframework.starter.common.exceptions.impl.InputErrorException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 销售出库单创建参数测试。
 */
class CreateSaleOutSheetVoTest {

  /**
   * 验证页面保存时验收数量不允许为负数。
   */
  @Test
  void validateShouldRejectNegativeConfirmNum() {
    CreateSaleOutSheetVo vo = createVo(new BigDecimal("-0.000001"));

    InputErrorException error = Assert.expectThrows(InputErrorException.class, () -> vo.validate(false));

    Assert.assertEquals(error.getMessage(), "第1行商品验收数量不允许小于0！");
  }

  /**
   * 验证页面保存时验收数量最多允许 6 位小数。
   */
  @Test
  void validateShouldRejectConfirmNumWithMoreThanSixDecimals() {
    CreateSaleOutSheetVo vo = createVo(new BigDecimal("1.1234567"));

    InputErrorException error = Assert.expectThrows(InputErrorException.class, () -> vo.validate(false));

    Assert.assertEquals(error.getMessage(), "第1行商品验收数量最多允许6位小数！");
  }

  /**
   * 创建销售出库单测试参数。
   *
   * @param confirmNum 验收数量
   * @return 创建参数
   */
  private CreateSaleOutSheetVo createVo(BigDecimal confirmNum) {
    SaleOutProductVo product = new SaleOutProductVo();
    product.setProductId("product-1");
    product.setOrderNum(BigDecimal.ONE);
    product.setTaxPrice(BigDecimal.ONE);
    product.setConfirmNum(confirmNum);

    CreateSaleOutSheetVo vo = new CreateSaleOutSheetVo();
    vo.setScId("sc-1");
    vo.setCustomerId("customer-1");
    vo.setOrderDate(LocalDate.now());
    vo.setRequired(false);
    vo.setProducts(Collections.singletonList(product));
    return vo;
  }
}
