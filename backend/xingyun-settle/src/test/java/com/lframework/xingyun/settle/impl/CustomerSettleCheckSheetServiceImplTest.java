package com.lframework.xingyun.settle.impl;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.sc.service.sale.SaleReturnService;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;
import com.lframework.xingyun.settle.entity.CustomerSettleCheckSheetDetail;
import com.lframework.xingyun.settle.enums.CustomerSettleCheckSheetBizType;
import com.lframework.xingyun.settle.mappers.CustomerSettleCheckSheetDetailMapper;
import com.lframework.xingyun.settle.mappers.CustomerSettleCheckSheetMapper;
import com.lframework.xingyun.settle.vo.check.customer.CreateCustomerSettleCheckSheetVo;
import com.lframework.xingyun.settle.vo.check.customer.CustomerSettleCheckSheetItemVo;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * 客户对账单直接确认服务测试。
 */
public class CustomerSettleCheckSheetServiceImplTest {

  /**
   * 混选销售出库和销售退货单时应拒绝确认对账。
   */
  @Test(expected = DefaultClientException.class)
  public void shouldRejectMixedSaleOutAndReturnItems() throws Exception {
    CustomerSettleCheckSheetServiceImpl service = newService();

    service.directApprovePass(createVo("customer-1", "100",
        item("sale-out-1", 1), item("sale-return-1", 2)));
  }

  /**
   * 两张金额五十的销售单确认一百零一时，应仅将差额平均加到最终对账金额。
   */
  @Test
  public void shouldAllocateDifferenceToEachCheckDetail() throws Exception {
    CustomerSettleCheckSheetServiceImpl service = newService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    CustomerSettleCheckSheetDetailMapper detailMapper = getField(service,
        "customerSettleCheckSheetDetailMapper");
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class)))
        .thenReturn(Arrays.asList(saleOutSheet("sale-out-1", "customer-1", "50"),
            saleOutSheet("sale-out-2", "customer-1", "50")));
    Mockito.when(detailMapper.insertBatch(Mockito.anyList())).thenReturn(2);
    Mockito.when(saleOutSheetService.setUnSettle(Mockito.anyString(),
        Mockito.eq(SettleStatus.UN_CHECK_BILL), Mockito.eq(0L))).thenReturn(1);

    service.directApprovePass(createVo("customer-1", "101",
        item("sale-out-1", 1), item("sale-out-2", 1)));

    ArgumentCaptor<List<CustomerSettleCheckSheetDetail>> detailCaptor = ArgumentCaptor.forClass(
        List.class);
    Mockito.verify(detailMapper).insertBatch(detailCaptor.capture());
    Assert.assertEquals(new BigDecimal("50.50"), detailCaptor.getValue().get(0).getPayAmount());
    Assert.assertEquals(new BigDecimal("50.50"), detailCaptor.getValue().get(1).getPayAmount());
    Assert.assertEquals(CustomerSettleCheckSheetBizType.OUT_SHEET,
        detailCaptor.getValue().get(0).getBizType());
  }

  /**
   * 源单状态或版本变化导致乐观锁更新失败时，应整体拒绝确认。
   */
  @Test(expected = DefaultClientException.class)
  public void shouldRejectWhenSourceSettleStatusOrVersionChanged() throws Exception {
    CustomerSettleCheckSheetServiceImpl service = newService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    CustomerSettleCheckSheetDetailMapper detailMapper = getField(service,
        "customerSettleCheckSheetDetailMapper");
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class)))
        .thenReturn(Arrays.asList(saleOutSheet("sale-out-1", "customer-1", "50"),
            saleOutSheet("sale-out-2", "customer-1", "50")));
    Mockito.when(detailMapper.insertBatch(Mockito.anyList())).thenReturn(2);
    Mockito.when(saleOutSheetService.setUnSettle(Mockito.anyString(),
        Mockito.eq(SettleStatus.UN_CHECK_BILL), Mockito.eq(0L))).thenReturn(1, 0);

    service.directApprovePass(createVo("customer-1", "100",
        item("sale-out-1", 1), item("sale-out-2", 1)));
  }

  /**
   * 确认对账加载销售源单时应走带数据权限的批量查询。
   */
  @Test
  public void shouldLoadSaleOutSourcesWithPermissionAwareQuery() throws Exception {
    CustomerSettleCheckSheetServiceImpl service = newService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    CustomerSettleCheckSheetDetailMapper detailMapper = getField(service,
        "customerSettleCheckSheetDetailMapper");
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class)))
        .thenReturn(Arrays.asList(saleOutSheet("sale-out-1", "customer-1", "50"),
            saleOutSheet("sale-out-2", "customer-1", "50")));
    Mockito.when(detailMapper.insertBatch(Mockito.anyList())).thenReturn(2);
    Mockito.when(saleOutSheetService.setUnSettle(Mockito.anyString(),
        Mockito.eq(SettleStatus.UN_CHECK_BILL), Mockito.eq(0L))).thenReturn(1);

    service.directApprovePass(createVo("customer-1", "100",
        item("sale-out-1", 1), item("sale-out-2", 1)));

    ArgumentCaptor<QuerySaleOutSheetVo> queryCaptor = ArgumentCaptor.forClass(
        QuerySaleOutSheetVo.class);
    Mockito.verify(saleOutSheetService).query(queryCaptor.capture());
    Assert.assertEquals(Arrays.asList("sale-out-1", "sale-out-2"),
        queryCaptor.getValue().getIdList());
    Assert.assertEquals(Boolean.TRUE, queryCaptor.getValue().getRequireTxIdNull());
  }

  /**
   * 创建带 Mock 依赖的客户对账单服务。
   */
  private CustomerSettleCheckSheetServiceImpl newService() throws Exception {
    CustomerSettleCheckSheetServiceImpl service = new CustomerSettleCheckSheetServiceImpl();
    injectField(service, "saleOutSheetService", Mockito.mock(SaleOutSheetService.class));
    injectField(service, "saleReturnService", Mockito.mock(SaleReturnService.class));
    CustomerSettleCheckSheetMapper checkSheetMapper = Mockito.mock(CustomerSettleCheckSheetMapper.class);
    Mockito.when(checkSheetMapper.insert(Mockito.any())).thenReturn(1);
    injectField(service, "baseMapper", checkSheetMapper);
    injectField(service, "customerSettleCheckSheetDetailMapper", Mockito.mock(
        CustomerSettleCheckSheetDetailMapper.class));
    return service;
  }

  /**
   * 创建客户对账确认请求。
   */
  private CreateCustomerSettleCheckSheetVo createVo(String customerId, String checkAmount,
      CustomerSettleCheckSheetItemVo... items) {
    CreateCustomerSettleCheckSheetVo vo = new CreateCustomerSettleCheckSheetVo();
    vo.setCustomerId(customerId);
    vo.setCheckAmount(new BigDecimal(checkAmount));
    vo.setDescription("测试对账");
    vo.setItems(Arrays.asList(items));
    return vo;
  }

  /**
   * 创建对账业务项。
   */
  private CustomerSettleCheckSheetItemVo item(String bizId, int bizType) {
    CustomerSettleCheckSheetItemVo item = new CustomerSettleCheckSheetItemVo();
    item.setBizId(bizId);
    item.setBizType(bizType);
    return item;
  }

  /**
   * 创建待对账销售出库单。
   */
  private SaleOutSheet saleOutSheet(String id, String customerId, String totalAmount) {
    SaleOutSheet sheet = new SaleOutSheet();
    sheet.setId(id);
    sheet.setCustomerId(customerId);
    sheet.setTotalAmount(new BigDecimal(totalAmount));
    sheet.setSettleStatus(SettleStatus.UN_CHECK_BILL);
    sheet.setSettleVersion(0L);
    return sheet;
  }

  /**
   * 注入目标对象字段。
   */
  private void injectField(Object target, String fieldName, Object value)
      throws ReflectiveOperationException {
    Class<?> type = target.getClass();
    while (type != null) {
      try {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
        return;
      } catch (NoSuchFieldException e) {
        type = type.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName);
  }

  /**
   * 获取目标对象字段。
   */
  @SuppressWarnings("unchecked")
  private <T> T getField(Object target, String fieldName) throws Exception {
    Class<?> type = target.getClass();
    while (type != null) {
      try {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
      } catch (NoSuchFieldException e) {
        type = type.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName);
  }
}
