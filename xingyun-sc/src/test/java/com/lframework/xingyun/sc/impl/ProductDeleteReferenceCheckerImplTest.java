package com.lframework.xingyun.sc.impl;

import com.lframework.xingyun.sc.mappers.PurchaseOrderDetailMapper;
import com.lframework.xingyun.sc.mappers.ReceiveSheetDetailMapper;
import com.lframework.xingyun.sc.mappers.SaleOrderDetailMapper;
import com.lframework.xingyun.sc.mappers.SaleOutSheetDetailMapper;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

class ProductDeleteReferenceCheckerImplTest {

  /**
   * 任一业务单据明细引用商品时，应阻止商品删除。
   */
  @Test(dataProvider = "referencedDocumentCounts")
  void shouldReportReferencedWhenAnyDocumentDetailExists(int purchaseOrderCount,
      int receiveSheetCount, int saleOrderCount, int saleOutSheetCount) {
    PurchaseOrderDetailMapper purchaseOrderMapper = Mockito.mock(PurchaseOrderDetailMapper.class);
    ReceiveSheetDetailMapper receiveSheetMapper = Mockito.mock(ReceiveSheetDetailMapper.class);
    SaleOrderDetailMapper saleOrderMapper = Mockito.mock(SaleOrderDetailMapper.class);
    SaleOutSheetDetailMapper saleOutSheetMapper = Mockito.mock(SaleOutSheetDetailMapper.class);
    Mockito.when(purchaseOrderMapper.selectCount(Mockito.any())).thenReturn(purchaseOrderCount);
    Mockito.when(receiveSheetMapper.selectCount(Mockito.any())).thenReturn(receiveSheetCount);
    Mockito.when(saleOrderMapper.selectCount(Mockito.any())).thenReturn(saleOrderCount);
    Mockito.when(saleOutSheetMapper.selectCount(Mockito.any())).thenReturn(saleOutSheetCount);
    ProductDeleteReferenceCheckerImpl checker = new ProductDeleteReferenceCheckerImpl(
        purchaseOrderMapper, receiveSheetMapper, saleOrderMapper, saleOutSheetMapper);

    Assert.assertTrue(checker.isReferenced("product-1"));
  }

  /**
   * 提供四类单据明细分别命中的测试数据。
   */
  @DataProvider
  Object[][] referencedDocumentCounts() {
    return new Object[][] {{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}};
  }

  /**
   * 四类业务单据明细均未引用商品时，应允许商品删除。
   */
  @Test
  void shouldReportNotReferencedWhenAllDocumentDetailsAreAbsent() {
    PurchaseOrderDetailMapper purchaseOrderMapper = Mockito.mock(PurchaseOrderDetailMapper.class);
    ReceiveSheetDetailMapper receiveSheetMapper = Mockito.mock(ReceiveSheetDetailMapper.class);
    SaleOrderDetailMapper saleOrderMapper = Mockito.mock(SaleOrderDetailMapper.class);
    SaleOutSheetDetailMapper saleOutSheetMapper = Mockito.mock(SaleOutSheetDetailMapper.class);
    Mockito.when(purchaseOrderMapper.selectCount(Mockito.any())).thenReturn(0);
    Mockito.when(receiveSheetMapper.selectCount(Mockito.any())).thenReturn(0);
    Mockito.when(saleOrderMapper.selectCount(Mockito.any())).thenReturn(0);
    Mockito.when(saleOutSheetMapper.selectCount(Mockito.any())).thenReturn(0);
    ProductDeleteReferenceCheckerImpl checker = new ProductDeleteReferenceCheckerImpl(
        purchaseOrderMapper, receiveSheetMapper, saleOrderMapper, saleOutSheetMapper);

    Assert.assertFalse(checker.isReferenced("product-1"));
    Mockito.verify(saleOutSheetMapper).selectCount(Mockito.any());
  }
}
