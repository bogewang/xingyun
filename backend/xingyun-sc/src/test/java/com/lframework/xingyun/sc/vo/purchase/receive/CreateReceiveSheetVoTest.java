package com.lframework.xingyun.sc.vo.purchase.receive;

import com.lframework.starter.common.exceptions.impl.InputErrorException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

class CreateReceiveSheetVoTest {

  @Test
  void validateShouldAllowNullPurchasePriceAndReceiveNum() {
    CreateReceiveSheetVo sheet = createSheet(null, null);

    sheet.validate(false);
  }

  @Test
  void validateShouldAllowZeroPurchasePriceAndReceiveNum() {
    CreateReceiveSheetVo sheet = createSheet(BigDecimal.ZERO, BigDecimal.ZERO);

    sheet.validate(false);
  }

  @Test
  void validateShouldDiscardProductsWithBlankProductId() {
    ReceiveProductVo nullProduct = new ReceiveProductVo();
    ReceiveProductVo blankProduct = new ReceiveProductVo();
    blankProduct.setProductId(" ");
    CreateReceiveSheetVo sheet = createSheet(BigDecimal.ZERO, BigDecimal.ZERO);
    sheet.setProducts(Arrays.asList(nullProduct, blankProduct, sheet.getProducts().get(0)));

    sheet.validate(false);

    Assert.assertEquals(sheet.getProducts().size(), 1);
    Assert.assertEquals(sheet.getProducts().get(0).getProductId(), "product-1");
  }

  @Test(expectedExceptions = InputErrorException.class)
  void validateShouldRejectNegativePurchasePrice() {
    CreateReceiveSheetVo sheet = createSheet(new BigDecimal("-0.01"), BigDecimal.ONE);

    sheet.validate(false);
  }

  @Test(expectedExceptions = InputErrorException.class)
  void validateShouldRejectNegativeReceiveNum() {
    CreateReceiveSheetVo sheet = createSheet(BigDecimal.ONE, new BigDecimal("-0.01"));

    sheet.validate(false);
  }

  private CreateReceiveSheetVo createSheet(BigDecimal purchasePrice, BigDecimal receiveNum) {
    ReceiveProductVo product = new ReceiveProductVo();
    product.setProductId("product-1");
    product.setPurchasePrice(purchasePrice);
    product.setReceiveNum(receiveNum);

    CreateReceiveSheetVo sheet = new CreateReceiveSheetVo();
    sheet.setScId("store-1");
    sheet.setSupplierId("supplier-1");
    sheet.setOrderDate(LocalDate.of(2026, 7, 14));
    sheet.setReceiveDate(LocalDate.of(2026, 7, 14));
    sheet.setProducts(Collections.singletonList(product));
    return sheet;
  }
}
