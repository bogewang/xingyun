package com.lframework.xingyun.sc.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.lframework.xingyun.sc.entity.PurchaseOrderDetail;
import com.lframework.xingyun.sc.entity.ReceiveSheetDetail;
import com.lframework.xingyun.sc.entity.SaleOrderDetail;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import com.lframework.xingyun.sc.mappers.PurchaseOrderDetailMapper;
import com.lframework.xingyun.sc.mappers.ReceiveSheetDetailMapper;
import com.lframework.xingyun.sc.mappers.SaleOrderDetailMapper;
import com.lframework.xingyun.sc.mappers.SaleOutSheetDetailMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.apache.ibatis.builder.MapperBuilderAssistant;
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
    assertInvocationsAndProductConditions(purchaseOrderCount, receiveSheetCount, saleOrderCount,
        saleOutSheetCount, purchaseOrderMapper, receiveSheetMapper, saleOrderMapper,
        saleOutSheetMapper);
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

  /**
   * 验证命中前的查询条件及命中后的短路调用。
   */
  private void assertInvocationsAndProductConditions(int purchaseOrderCount, int receiveSheetCount,
      int saleOrderCount, int saleOutSheetCount, PurchaseOrderDetailMapper purchaseOrderMapper,
      ReceiveSheetDetailMapper receiveSheetMapper, SaleOrderDetailMapper saleOrderMapper,
      SaleOutSheetDetailMapper saleOutSheetMapper) {
    ArgumentCaptor<LambdaQueryWrapper<PurchaseOrderDetail>> purchaseOrderCaptor =
        createQueryWrapperCaptor();
    Mockito.verify(purchaseOrderMapper).selectCount(purchaseOrderCaptor.capture());
    assertProductIdCondition(purchaseOrderCaptor.getValue(), PurchaseOrderDetail.class);
    if (purchaseOrderCount > 0) {
      Mockito.verifyNoInteractions(receiveSheetMapper, saleOrderMapper, saleOutSheetMapper);
      return;
    }

    ArgumentCaptor<LambdaQueryWrapper<ReceiveSheetDetail>> receiveSheetCaptor =
        createQueryWrapperCaptor();
    Mockito.verify(receiveSheetMapper).selectCount(receiveSheetCaptor.capture());
    assertProductIdCondition(receiveSheetCaptor.getValue(), ReceiveSheetDetail.class);
    if (receiveSheetCount > 0) {
      Mockito.verifyNoInteractions(saleOrderMapper, saleOutSheetMapper);
      return;
    }

    ArgumentCaptor<LambdaQueryWrapper<SaleOrderDetail>> saleOrderCaptor = createQueryWrapperCaptor();
    Mockito.verify(saleOrderMapper).selectCount(saleOrderCaptor.capture());
    assertProductIdCondition(saleOrderCaptor.getValue(), SaleOrderDetail.class);
    if (saleOrderCount > 0) {
      Mockito.verifyNoInteractions(saleOutSheetMapper);
      return;
    }

    ArgumentCaptor<LambdaQueryWrapper<SaleOutSheetDetail>> saleOutSheetCaptor =
        createQueryWrapperCaptor();
    Mockito.verify(saleOutSheetMapper).selectCount(saleOutSheetCaptor.capture());
    assertProductIdCondition(saleOutSheetCaptor.getValue(), SaleOutSheetDetail.class);
    Assert.assertTrue(saleOutSheetCount > 0);
  }

  /**
   * 创建 Lambda 查询条件参数捕获器。
   *
   * @return 查询条件捕获器
   */
  @SuppressWarnings("unchecked")
  private <T> ArgumentCaptor<LambdaQueryWrapper<T>> createQueryWrapperCaptor() {
    return (ArgumentCaptor<LambdaQueryWrapper<T>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(
        LambdaQueryWrapper.class);
  }

  /**
   * 初始化实体元数据后，验证查询条件使用 product_id 字段过滤指定商品。
   *
   * @param queryWrapper 被 Mapper 接收的查询条件
   * @param entityClass 查询实体类型
   */
  private void assertProductIdCondition(LambdaQueryWrapper<?> queryWrapper, Class<?> entityClass) {
    TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"),
        entityClass);
    Assert.assertEquals(queryWrapper.getEntityClass(), entityClass);
    Assert.assertTrue(queryWrapper.getSqlSegment().contains("product_id"));
    Assert.assertTrue(queryWrapper.getParamNameValuePairs().containsValue("product-1"));
  }
}
