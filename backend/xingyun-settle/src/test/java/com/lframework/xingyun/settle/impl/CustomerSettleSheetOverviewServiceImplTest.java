package com.lframework.xingyun.settle.impl;

import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.entity.SaleReturn;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.sc.service.sale.SaleReturnService;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;
import com.lframework.xingyun.sc.vo.sale.returned.QuerySaleReturnVo;
import com.lframework.xingyun.settle.bo.sheet.customer.CustomerSettleOverviewBo;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSettleOverviewVo;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * 客户结算总览服务测试。
 */
public class CustomerSettleSheetOverviewServiceImplTest {

  /**
   * 销售出库与销售退货应按客户和结算状态聚合。
   */
  @Test
  public void shouldGroupBothBizTypesByCustomerAndStatus() throws Exception {
    CustomerSettleSheetServiceImpl service = overviewService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    SaleReturnService saleReturnService = getField(service, "saleReturnService");
    CustomerService customerService = getField(service, "customerService");
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class))).thenReturn(
        Collections.singletonList(saleOutSheet("out-1", "customer-1", "100.00",
            SettleStatus.UN_CHECK_BILL)));
    Mockito.when(saleReturnService.query(Mockito.any(QuerySaleReturnVo.class))).thenReturn(
        Collections.singletonList(saleReturn("return-1", "customer-1", "20.00",
            SettleStatus.UN_SETTLE)));
    Mockito.when(customerService.listByIds(Mockito.anyCollection())).thenReturn(
        Collections.singletonList(customer("customer-1", "C001", "客户一")));

    CustomerSettleOverviewBo row = service.querySettleOverviews(queryVo(1, 20)).getDatas()
        .get(0);

    Assert.assertEquals("customer-1", row.getCustomerId());
    Assert.assertEquals(Integer.valueOf(1), row.getUnCheckCount());
    Assert.assertEquals(new BigDecimal("100.00"), row.getUnCheckAmount());
    Assert.assertEquals(Integer.valueOf(1), row.getUnSettleCount());
    Assert.assertEquals(new BigDecimal("-20.00"), row.getUnSettleAmount());
  }

  /**
   * 销售退货金额应在总览中保留负数方向。
   */
  @Test
  public void shouldKeepReturnAmountNegative() throws Exception {
    CustomerSettleSheetServiceImpl service = overviewService();
    SaleReturnService saleReturnService = getField(service, "saleReturnService");
    CustomerService customerService = getField(service, "customerService");
    Mockito.when(saleReturnService.query(Mockito.any(QuerySaleReturnVo.class))).thenReturn(
        Collections.singletonList(saleReturn("return-1", "customer-1", "12.50",
            SettleStatus.PART_SETTLE)));
    Mockito.when(customerService.listByIds(Mockito.anyCollection())).thenReturn(
        Collections.singletonList(customer("customer-1", "C001", "客户一")));

    CustomerSettleOverviewBo row = service.querySettleOverviews(queryVo(1, 20)).getDatas()
        .get(0);

    Assert.assertEquals(new BigDecimal("-12.50"), row.getPartSettleAmount());
  }

  /**
   * 总览源单查询必须排除已被结算交易占用的单据，并透传客户条件。
   */
  @Test
  public void shouldExcludeTxOccupiedSheets() throws Exception {
    CustomerSettleSheetServiceImpl service = overviewService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    SaleReturnService saleReturnService = getField(service, "saleReturnService");
    CustomerService customerService = getField(service, "customerService");
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class)))
        .thenReturn(Collections.emptyList());
    Mockito.when(saleReturnService.query(Mockito.any(QuerySaleReturnVo.class)))
        .thenReturn(Collections.emptyList());
    Mockito.when(customerService.listByIds(Mockito.anyCollection()))
        .thenReturn(Collections.emptyList());
    QueryCustomerSettleOverviewVo vo = queryVo(1, 20);
    vo.setCustomerId("customer-1");

    service.querySettleOverviews(vo);

    ArgumentCaptor<QuerySaleOutSheetVo> saleOutCaptor = ArgumentCaptor.forClass(
        QuerySaleOutSheetVo.class);
    ArgumentCaptor<QuerySaleReturnVo> saleReturnCaptor = ArgumentCaptor.forClass(
        QuerySaleReturnVo.class);
    Mockito.verify(saleOutSheetService).query(saleOutCaptor.capture());
    Mockito.verify(saleReturnService).query(saleReturnCaptor.capture());
    Assert.assertEquals(Boolean.TRUE, saleOutCaptor.getValue().getRequireTxIdNull());
    Assert.assertEquals(Boolean.TRUE, saleReturnCaptor.getValue().getRequireTxIdNull());
    Assert.assertEquals("customer-1", saleOutCaptor.getValue().getCustomerId());
    Assert.assertEquals("customer-1", saleReturnCaptor.getValue().getCustomerId());
  }

  /**
   * 总览应在客户聚合、排序后按客户行分页。
   */
  @Test
  public void shouldPageGroupedCustomerRows() throws Exception {
    CustomerSettleSheetServiceImpl service = overviewService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    CustomerService customerService = getField(service, "customerService");
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class))).thenReturn(
        Arrays.asList(
            saleOutSheet("out-1", "customer-1", "10", SettleStatus.UN_SETTLE),
            saleOutSheet("out-2", "customer-2", "20", SettleStatus.SETTLED),
            saleOutSheet("out-3", "customer-3", "30", SettleStatus.PART_SETTLE)));
    Mockito.when(customerService.listByIds(Mockito.anyCollection())).thenReturn(Arrays.asList(
        customer("customer-1", "C001", "客户甲"),
        customer("customer-2", "C002", "客户乙"),
        customer("customer-3", "C003", "客户丙")));

    PageResult<CustomerSettleOverviewBo> result = service.querySettleOverviews(queryVo(2, 1));

    Assert.assertEquals(3L, result.getTotalCount());
    Assert.assertEquals(1, result.getDatas().size());
    Assert.assertEquals("customer-2", result.getDatas().get(0).getCustomerId());
  }

  /**
   * 创建总览查询条件。
   */
  private QueryCustomerSettleOverviewVo queryVo(int pageIndex, int pageSize) {
    QueryCustomerSettleOverviewVo vo = new QueryCustomerSettleOverviewVo();
    vo.setPageIndex(pageIndex);
    vo.setPageSize(pageSize);
    return vo;
  }

  /**
   * 创建测试服务及可观测依赖。
   */
  private CustomerSettleSheetServiceImpl overviewService() throws Exception {
    CustomerSettleSheetServiceImpl service = new CustomerSettleSheetServiceImpl();
    injectField(service, "saleOutSheetService", Mockito.mock(SaleOutSheetService.class));
    injectField(service, "saleReturnService", Mockito.mock(SaleReturnService.class));
    injectField(service, "customerService", Mockito.mock(CustomerService.class));
    return service;
  }

  /**
   * 创建销售出库单测试数据。
   */
  private SaleOutSheet saleOutSheet(String id, String customerId, String totalAmount,
      SettleStatus settleStatus) {
    SaleOutSheet sheet = new SaleOutSheet();
    sheet.setId(id);
    sheet.setCustomerId(customerId);
    sheet.setTotalAmount(new BigDecimal(totalAmount));
    sheet.setSettleStatus(settleStatus);
    return sheet;
  }

  /**
   * 创建销售退货单测试数据。
   */
  private SaleReturn saleReturn(String id, String customerId, String totalAmount,
      SettleStatus settleStatus) {
    SaleReturn sheet = new SaleReturn();
    sheet.setId(id);
    sheet.setCustomerId(customerId);
    sheet.setTotalAmount(new BigDecimal(totalAmount));
    sheet.setSettleStatus(settleStatus);
    return sheet;
  }

  /**
   * 创建客户测试数据。
   */
  private Customer customer(String id, String code, String name) {
    Customer customer = new Customer();
    customer.setId(id);
    customer.setCode(code);
    customer.setName(name);
    return customer;
  }

  /**
   * 反射注入私有字段。
   */
  private void injectField(Object target, String fieldName, Object value) throws Exception {
    Field field = CustomerSettleSheetServiceImpl.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  /**
   * 读取私有字段。
   */
  @SuppressWarnings("unchecked")
  private <T> T getField(Object target, String fieldName) throws Exception {
    Field field = CustomerSettleSheetServiceImpl.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    return (T) field.get(target);
  }
}
