package com.lframework.xingyun.settle.impl;

import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.sc.service.sale.SaleReturnService;
import com.lframework.xingyun.settle.bo.sheet.customer.CustomerSaleSettleInfoBo;
import com.lframework.xingyun.settle.entity.CustomerSettleSheet;
import com.lframework.xingyun.settle.entity.CustomerSettleSheetDetail;
import com.lframework.xingyun.settle.enums.CustomerSettleSheetStatus;
import com.lframework.xingyun.settle.mappers.CustomerSettleSheetMapper;
import com.lframework.xingyun.settle.service.CustomerSettleSheetDetailService;
import com.lframework.xingyun.settle.dto.sheet.customer.CustomerSettleSheetFullDto;
import com.lframework.xingyun.settle.vo.sheet.customer.CreateCustomerSettleSheetVo;
import com.lframework.xingyun.settle.vo.sheet.customer.CustomerSettleSheetItemVo;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSaleSettleInfoVo;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * 客户结算工作台服务测试。
 */
public class CustomerSettleSheetServiceImplTest {

  /**
   * 源单版本变化后，第二次按同一旧余额结算必须失败且不得再次保存明细。
   */
  @Test
  public void directApprovePassShouldRejectSecondSubmissionWithStaleBizVersion() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    CustomerSettleSheetDetailService detailService = getField(service,
        "customerSettleSheetDetailService");
    SaleOutSheet sheet = saleOutSheet("sale-1", "customer-1", "100", "0",
        SettleStatus.UN_SETTLE);
    sheet.setUpdateTime(LocalDateTime.of(2026, 7, 25, 20, 0));
    Mockito.when(saleOutSheetService.listByIds(Mockito.anyCollection())).thenReturn(
        Collections.singletonList(sheet));
    Mockito.when(saleOutSheetService.setPartSettle(Mockito.eq("sale-1"),
        Mockito.eq(SettleStatus.UN_SETTLE), Mockito.eq(sheet.getUpdateTime()))).thenReturn(1, 0);
    Mockito.when(detailService.saveBatch(Mockito.anyCollection())).thenReturn(true);

    service.directApprovePass(directSettleVo("customer-1", "60", item("sale-1", 1)));
    try {
      service.directApprovePass(directSettleVo("customer-1", "60", item("sale-1", 1)));
      Assert.fail("源单版本已变化时应拒绝再次结算");
    } catch (DefaultClientException e) {
      // 预期异常。
    }

    Mockito.verify(detailService, Mockito.times(1)).saveBatch(Mockito.anyCollection());
  }

  /**
   * 直接结算金额精度不得超过两位小数。
   */
  @Test(expected = DefaultClientException.class)
  public void directApprovePassShouldRejectAmountWithMoreThanTwoDecimalPlaces() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    CustomerSettleSheetDetailService detailService = getField(service,
        "customerSettleSheetDetailService");
    SaleOutSheet sheet = saleOutSheet("sale-1", "customer-1", "10", "0",
        SettleStatus.UN_SETTLE);
    Mockito.when(saleOutSheetService.listByIds(Mockito.anyCollection())).thenReturn(
        Collections.singletonList(sheet));
    Mockito.when(saleOutSheetService.setPartSettle(Mockito.eq("sale-1"),
        Mockito.eq(SettleStatus.UN_SETTLE), Mockito.isNull())).thenReturn(1);
    Mockito.when(detailService.saveBatch(Mockito.anyCollection())).thenReturn(true);

    service.directApprovePass(directSettleVo("customer-1", "0.001", item("sale-1", 1)));
  }

  /**
   * 详情查询应批量从销售源单补齐业务单号。
   */
  @Test
  public void getDetailShouldFillBizCodesFromSaleSources() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    CustomerSettleSheetMapper mapper = getField(service, "baseMapper");
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    CustomerSettleSheetFullDto detail = new CustomerSettleSheetFullDto();
    CustomerSettleSheetFullDto.SheetDetailDto item =
        new CustomerSettleSheetFullDto.SheetDetailDto();
    item.setBizId("sale-1");
    detail.setDetails(Collections.singletonList(item));
    Mockito.when(mapper.getDetail("sheet-1")).thenReturn(detail);
    Mockito.when(saleOutSheetService.listByIds(Mockito.anyCollection())).thenReturn(
        Collections.singletonList(saleOutSheet("sale-1", "customer-1", "10", "0",
            SettleStatus.UN_SETTLE)));

    CustomerSettleSheetFullDto result = service.getDetail("sheet-1");

    Assert.assertEquals("sale-1", result.getDetails().get(0).getBizCode());
  }

  /**
   * 直接结算时不允许混入其他客户的销售单。
   */
  @Test(expected = DefaultClientException.class)
  public void directApprovePassShouldRejectDifferentCustomers() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    Mockito.when(saleOutSheetService.listByIds(Mockito.anyCollection())).thenReturn(
        Collections.singletonList(saleOutSheet("sale-1", "customer-2", "10", "0",
            SettleStatus.UN_SETTLE)));

    service.directApprovePass(directSettleVo("customer-1", "10", item("sale-1", 1)));
  }

  /**
   * 直接结算金额必须为非负数。
   */
  @Test(expected = DefaultClientException.class)
  public void directApprovePassShouldRejectNegativeSettleAmount() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();

    service.directApprovePass(directSettleVo("customer-1", "-0.01", item("sale-1", 1)));
  }

  /**
   * 已结算业务单据不能再次直接结算。
   */
  @Test(expected = DefaultClientException.class)
  public void directApprovePassShouldRejectSettledBizItem() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    Mockito.when(saleOutSheetService.listByIds(Mockito.anyCollection())).thenReturn(
        Collections.singletonList(saleOutSheet("sale-1", "customer-1", "10", "0",
            SettleStatus.SETTLED)));

    service.directApprovePass(directSettleVo("customer-1", "10", item("sale-1", 1)));
  }

  /**
   * 确认结算金额不得超过所选业务单据的未结算总额。
   */
  @Test(expected = DefaultClientException.class)
  public void directApprovePassShouldRejectAmountGreaterThanUnSettleTotal() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    Mockito.when(saleOutSheetService.listByIds(Mockito.anyCollection())).thenReturn(
        Collections.singletonList(saleOutSheet("sale-1", "customer-1", "10", "0",
            SettleStatus.UN_SETTLE)));
    Mockito.when(saleOutSheetService.setSettled(Mockito.eq("sale-1"),
        Mockito.eq(SettleStatus.UN_SETTLE), Mockito.isNull())).thenReturn(1);
    CustomerSettleSheetDetailService detailService = getField(service,
        "customerSettleSheetDetailService");
    Mockito.when(detailService.saveBatch(Mockito.anyCollection())).thenReturn(true);

    service.directApprovePass(directSettleVo("customer-1", "10.01", item("sale-1", 1)));
  }

  /**
   * 源单状态更新失败时，应中止保存结算明细。
   */
  @Test
  public void directApprovePassShouldAbortWhenBizStatusUpdateFails() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    CustomerSettleSheetDetailService detailService = getField(service,
        "customerSettleSheetDetailService");
    Mockito.when(saleOutSheetService.listByIds(Mockito.anyCollection())).thenReturn(
        Collections.singletonList(saleOutSheet("sale-1", "customer-1", "10", "0",
            SettleStatus.UN_SETTLE)));
    Mockito.when(saleOutSheetService.setSettled(Mockito.eq("sale-1"),
        Mockito.eq(SettleStatus.UN_SETTLE), Mockito.isNull())).thenReturn(0);

    try {
      service.directApprovePass(directSettleVo("customer-1", "10", item("sale-1", 1)));
      Assert.fail("源单状态更新失败时应抛出业务异常");
    } catch (DefaultClientException e) {
      // 预期异常。
    }

    Mockito.verify(detailService, Mockito.never()).saveBatch(Mockito.anyCollection());
  }

  /**
   * 直接结算应按当前未结算金额分摊，且明细合计严格等于确认金额。
   */
  @Test
  public void directApprovePassShouldAllocateAmountsToRequestedTotal() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    CustomerSettleSheetDetailService detailService = getField(service,
        "customerSettleSheetDetailService");
    Mockito.when(saleOutSheetService.listByIds(Mockito.anyCollection())).thenReturn(Arrays.asList(
        saleOutSheet("sale-1", "customer-1", "10", "0", SettleStatus.UN_SETTLE),
        saleOutSheet("sale-2", "customer-1", "20", "0", SettleStatus.UN_SETTLE)));
    Mockito.when(saleOutSheetService.setPartSettle(Mockito.anyString(),
        Mockito.eq(SettleStatus.UN_SETTLE), Mockito.isNull())).thenReturn(1);
    Mockito.when(detailService.saveBatch(Mockito.anyCollection())).thenReturn(true);

    service.directApprovePass(directSettleVo("customer-1", "24", item("sale-1", 1),
        item("sale-2", 1)));

    org.mockito.ArgumentCaptor<Collection<CustomerSettleSheetDetail>> captor =
        org.mockito.ArgumentCaptor.forClass(Collection.class);
    Mockito.verify(detailService).saveBatch(captor.capture());
    BigDecimal total = captor.getValue().stream().map(CustomerSettleSheetDetail::getPayAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    Assert.assertEquals(0, new BigDecimal("24.00").compareTo(total));
  }

  /**
   * 汇总结算金额时，仅统计审核通过的客户结算单明细。
   */
  @Test
  public void shouldOnlySumApprovedSettleSheetDetails() throws Exception {
    CustomerSettleSheetServiceImpl service = new CustomerSettleSheetServiceImpl();
    SaleOutSheetService saleOutSheetService = Mockito.mock(SaleOutSheetService.class);
    CustomerSettleSheetDetailService detailService = Mockito.mock(
        CustomerSettleSheetDetailService.class);
    CustomerSettleSheetMapper mapper = Mockito.mock(CustomerSettleSheetMapper.class);
    CustomerService customerService = Mockito.mock(CustomerService.class);
    injectField(service, "saleOutSheetService", saleOutSheetService);
    injectField(service, "customerSettleSheetDetailService", detailService);
    injectField(service, "customerService", customerService);
    injectField(service, "baseMapper", mapper);

    QueryCustomerSaleSettleInfoVo vo = new QueryCustomerSaleSettleInfoVo();
    vo.setBizType(1);
    vo.setPageIndex(1);
    vo.setPageSize(20);
    SaleOutSheet saleOutSheet = new SaleOutSheet();
    saleOutSheet.setId("sale-1");
    saleOutSheet.setCode("SO-001");
    saleOutSheet.setCustomerId("customer-1");
    saleOutSheet.setTotalAmount(new BigDecimal("50"));
    saleOutSheet.setPaidAmount(BigDecimal.ZERO);
    Mockito.when(saleOutSheetService.query(Mockito.eq(1), Mockito.eq(20), Mockito.any()))
        .thenReturn(PageResultUtil.newInstance(1, 20, 3, Collections.singletonList(saleOutSheet)));
    Customer customer = new Customer();
    customer.setId("customer-1");
    customer.setName("客户A");
    Mockito.when(customerService.listByIds(Mockito.anyCollection()))
        .thenReturn(Collections.singletonList(customer));

    CustomerSettleSheetDetail approvedDetail = detail("approved-sheet", "sale-1", "10");
    CustomerSettleSheetDetail draftDetail = detail("draft-sheet", "sale-1", "5");
    Mockito.when(detailService.list(Mockito.any())).thenReturn(Arrays.asList(approvedDetail, draftDetail));
    Mockito.when(mapper.selectBatchIds(Mockito.anyCollection())).thenReturn(Arrays.asList(
        settleSheet("approved-sheet", CustomerSettleSheetStatus.APPROVE_PASS),
        settleSheet("draft-sheet", CustomerSettleSheetStatus.CREATED)));

    PageResult<CustomerSaleSettleInfoBo> result = service.querySaleSettleInfos(vo);

    Assert.assertEquals(3L, result.getTotalCount());
    Assert.assertEquals(new BigDecimal("10"), result.getDatas().get(0).getSettleAmount());
    Assert.assertEquals(new BigDecimal("40"), result.getDatas().get(0).getUnSettleAmount());
  }

  /**
   * 工作台查询应沿用销售来源的分页总数，避免全量加载后在 JVM 截页。
   */
  @Test
  public void shouldKeepSourcePageTotalWhenQuerySaleSettleInfos() throws Exception {
    CustomerSettleSheetServiceImpl service = new CustomerSettleSheetServiceImpl();
    SaleOutSheetService saleOutSheetService = Mockito.mock(SaleOutSheetService.class);
    CustomerSettleSheetDetailService detailService = Mockito.mock(
        CustomerSettleSheetDetailService.class);
    CustomerSettleSheetMapper mapper = Mockito.mock(CustomerSettleSheetMapper.class);
    CustomerService customerService = Mockito.mock(CustomerService.class);
    injectField(service, "saleOutSheetService", saleOutSheetService);
    injectField(service, "customerSettleSheetDetailService", detailService);
    injectField(service, "customerService", customerService);
    injectField(service, "baseMapper", mapper);

    QueryCustomerSaleSettleInfoVo vo = new QueryCustomerSaleSettleInfoVo();
    vo.setBizType(1);
    vo.setPageIndex(2);
    vo.setPageSize(1);
    SaleOutSheet saleOutSheet = new SaleOutSheet();
    saleOutSheet.setId("sale-2");
    saleOutSheet.setCustomerId("customer-2");
    saleOutSheet.setTotalAmount(BigDecimal.ONE);
    Mockito.when(saleOutSheetService.query(Mockito.eq(2), Mockito.eq(1), Mockito.any()))
        .thenReturn(PageResultUtil.newInstance(2, 1, 3, Collections.singletonList(saleOutSheet)));
    Mockito.when(customerService.listByIds(Mockito.anyCollection()))
        .thenReturn(Collections.emptyList());
    Mockito.when(detailService.list(Mockito.any())).thenReturn(Collections.emptyList());

    PageResult<CustomerSaleSettleInfoBo> result = service.querySaleSettleInfos(vo);

    Assert.assertEquals(3L, result.getTotalCount());
    Assert.assertEquals(1, result.getDatas().size());
    Assert.assertEquals("sale-2", result.getDatas().get(0).getId());
  }

  /**
   * 创建客户结算单明细。
   */
  private CustomerSettleSheetDetail detail(String sheetId, String bizId, String payAmount) {
    CustomerSettleSheetDetail detail = new CustomerSettleSheetDetail();
    detail.setSheetId(sheetId);
    detail.setBizId(bizId);
    detail.setPayAmount(new BigDecimal(payAmount));
    return detail;
  }

  /**
   * 创建直接结算服务及其可观测依赖。
   */
  private CustomerSettleSheetServiceImpl directSettleService() throws Exception {
    CustomerSettleSheetServiceImpl service = new CustomerSettleSheetServiceImpl();
    injectField(service, "saleOutSheetService", Mockito.mock(SaleOutSheetService.class));
    injectField(service, "saleReturnService", Mockito.mock(SaleReturnService.class));
    injectField(service, "customerSettleSheetDetailService", Mockito.mock(
        CustomerSettleSheetDetailService.class));
    CustomerSettleSheetMapper mapper = Mockito.mock(CustomerSettleSheetMapper.class);
    Mockito.when(mapper.insert(Mockito.any(CustomerSettleSheet.class))).thenReturn(1);
    injectField(service, "baseMapper", mapper);
    injectField(service, "generateCodeService", Mockito.mock(
        com.lframework.starter.web.inner.service.GenerateCodeService.class));
    return service;
  }

  /**
   * 创建销售出库单测试数据。
   */
  private SaleOutSheet saleOutSheet(String id, String customerId, String totalAmount,
      String paidAmount, SettleStatus settleStatus) {
    SaleOutSheet sheet = new SaleOutSheet();
    sheet.setId(id);
    sheet.setCode(id);
    sheet.setCustomerId(customerId);
    sheet.setTotalAmount(new BigDecimal(totalAmount));
    sheet.setPaidAmount(new BigDecimal(paidAmount));
    sheet.setSettleStatus(settleStatus);
    return sheet;
  }

  /**
   * 创建直接结算请求。
   */
  private CreateCustomerSettleSheetVo directSettleVo(String customerId, String settleAmount,
      CustomerSettleSheetItemVo... items) {
    CreateCustomerSettleSheetVo vo = new CreateCustomerSettleSheetVo();
    vo.setCustomerId(customerId);
    vo.setSettleAmount(new BigDecimal(settleAmount));
    vo.setItems(Arrays.asList(items));
    return vo;
  }

  /**
   * 创建业务单据选择项。
   */
  private CustomerSettleSheetItemVo item(String bizId, int bizType) {
    CustomerSettleSheetItemVo item = new CustomerSettleSheetItemVo();
    item.setBizId(bizId);
    item.setBizType(bizType);
    return item;
  }

  /**
   * 创建客户结算单。
   */
  private CustomerSettleSheet settleSheet(String id, CustomerSettleSheetStatus status) {
    CustomerSettleSheet sheet = new CustomerSettleSheet();
    sheet.setId(id);
    sheet.setStatus(status);
    return sheet;
  }

  /**
   * 向目标对象（含父类）注入测试依赖。
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
   * 读取目标对象（含父类）的注入字段。
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
