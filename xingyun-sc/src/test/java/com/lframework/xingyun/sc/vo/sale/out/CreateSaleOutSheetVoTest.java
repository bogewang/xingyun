package com.lframework.xingyun.sc.vo.sale.out;

import com.lframework.starter.common.exceptions.impl.InputErrorException;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

class CreateSaleOutSheetVoTest {

  @Test
  void validateShouldAllowNullTaxPriceAndOrderNum() {
    CreateSaleOutSheetVo sheet = createSheet(null, null);

    sheet.validate(false);
  }

  @Test
  void validateShouldAllowZeroTaxPriceAndOrderNum() {
    CreateSaleOutSheetVo sheet = createSheet(BigDecimal.ZERO, BigDecimal.ZERO);

    sheet.validate(false);
  }

  @Test(expectedExceptions = InputErrorException.class)
  void validateShouldRejectNegativeTaxPrice() {
    CreateSaleOutSheetVo sheet = createSheet(new BigDecimal("-0.01"), BigDecimal.ONE);

    sheet.validate(false);
  }

  @Test(expectedExceptions = InputErrorException.class)
  void validateShouldRejectNegativeOrderNum() {
    CreateSaleOutSheetVo sheet = createSheet(BigDecimal.ONE, new BigDecimal("-0.01"));

    sheet.validate(false);
  }

  private CreateSaleOutSheetVo createSheet(BigDecimal taxPrice, BigDecimal orderNum) {
    SaleOutProductVo product = new SaleOutProductVo();
    product.setProductId("product-1");
    product.setTaxPrice(taxPrice);
    product.setOrderNum(orderNum);

    CreateSaleOutSheetVo sheet = new CreateSaleOutSheetVo();
    sheet.setScId("store-1");
    sheet.setCustomerId("customer-1");
    sheet.setOrderDate(LocalDate.of(2026, 7, 14));
    sheet.setProducts(Collections.singletonList(product));
    return sheet;
  }
}
