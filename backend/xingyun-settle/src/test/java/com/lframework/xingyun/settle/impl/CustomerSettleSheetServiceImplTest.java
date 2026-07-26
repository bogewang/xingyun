package com.lframework.xingyun.settle.impl;

import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.annotations.oplog.OpLog;
import com.lframework.starter.web.core.annotations.timeline.OrderTimeLineLog;
import com.lframework.starter.web.inner.entity.OrderTimeLine;
import com.lframework.starter.web.inner.service.OrderTimeLineService;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.web.core.components.security.DefaultUserDetails;
import com.lframework.starter.web.core.components.security.SecurityUtil;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.core.components.timeline.ReceiveOrderTimeLineBizType;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.entity.SaleReturn;
import com.lframework.xingyun.sc.enums.SaleOutSheetStatus;
import com.lframework.xingyun.sc.enums.SaleReturnStatus;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.sc.service.sale.SaleReturnService;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;
import com.lframework.xingyun.sc.vo.sale.returned.QuerySaleReturnVo;
import com.lframework.xingyun.settle.bo.sheet.customer.CustomerSaleSettleInfoBo;
import com.lframework.xingyun.settle.bo.sheet.customer.GetCustomerSettleSheetBo;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * 客户结算工作台服务测试。
 */
public class CustomerSettleSheetServiceImplTest {

  /**
   * 工作台销售出库查询允许任意审核状态，但必须限制未被交易占用。
   */
  @Test
  public void querySaleOutWorkbenchShouldAllowAnyStatusAndRequireUnoccupiedSources()
      throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    CustomerService customerService = getField(service, "customerService");
    Mockito.when(saleOutSheetService.query(Mockito.eq(1), Mockito.eq(20), Mockito.any()))
        .thenReturn(PageResultUtil.newInstance(1, 20, 0, Collections.emptyList()));
    Mockito.when(customerService.listByIds(Mockito.anyCollection()))
        .thenReturn(Collections.emptyList());
    QueryCustomerSaleSettleInfoVo vo = new QueryCustomerSaleSettleInfoVo();
    vo.setBizType(1);
    vo.setPageIndex(1);
    vo.setPageSize(20);

    service.querySaleSettleInfos(vo);

    ArgumentCaptor<QuerySaleOutSheetVo> captor = ArgumentCaptor.forClass(
        QuerySaleOutSheetVo.class);
    Mockito.verify(saleOutSheetService).query(Mockito.eq(1), Mockito.eq(20), captor.capture());
    Assert.assertNull(captor.getValue().getStatus());
    Assert.assertEquals(Boolean.TRUE, captor.getValue().getRequireTxIdNull());
  }

  /**
   * 工作台销售退货查询允许任意审核状态，但必须限制未被交易占用。
   */
  @Test
  public void querySaleReturnWorkbenchShouldAllowAnyStatusAndRequireUnoccupiedSources()
      throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleReturnService saleReturnService = getField(service, "saleReturnService");
    CustomerService customerService = getField(service, "customerService");
    Mockito.when(saleReturnService.query(Mockito.eq(1), Mockito.eq(20), Mockito.any()))
        .thenReturn(PageResultUtil.newInstance(1, 20, 0, Collections.emptyList()));
    Mockito.when(customerService.listByIds(Mockito.anyCollection()))
        .thenReturn(Collections.emptyList());
    QueryCustomerSaleSettleInfoVo vo = new QueryCustomerSaleSettleInfoVo();
    vo.setBizType(2);
    vo.setPageIndex(1);
    vo.setPageSize(20);

    service.querySaleSettleInfos(vo);

    ArgumentCaptor<QuerySaleReturnVo> captor = ArgumentCaptor.forClass(QuerySaleReturnVo.class);
    Mockito.verify(saleReturnService).query(Mockito.eq(1), Mockito.eq(20), captor.capture());
    Assert.assertNull(captor.getValue().getStatus());
    Assert.assertEquals(Boolean.TRUE, captor.getValue().getRequireTxIdNull());
  }

  /**
   * 销售退货工作台金额必须按退款负方向展示。
   */
  @Test
  public void querySaleReturnWorkbenchShouldExposeNegativeAmounts() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleReturnService saleReturnService = getField(service, "saleReturnService");
    CustomerService customerService = getField(service, "customerService");
    SaleReturn saleReturn = saleReturn("return-1", "customer-1", "20",
        SettleStatus.UN_SETTLE);
    Mockito.when(saleReturnService.query(Mockito.eq(1), Mockito.eq(20), Mockito.any()))
        .thenReturn(PageResultUtil.newInstance(1, 20, 1,
            Collections.singletonList(saleReturn)));
    Mockito.when(customerService.listByIds(Mockito.anyCollection()))
        .thenReturn(Collections.emptyList());
    QueryCustomerSaleSettleInfoVo vo = new QueryCustomerSaleSettleInfoVo();
    vo.setBizType(2);
    vo.setPageIndex(1);
    vo.setPageSize(20);

    PageResult<CustomerSaleSettleInfoBo> result = service.querySaleSettleInfos(vo);

    Assert.assertEquals(new BigDecimal("-20"), result.getDatas().get(0).getTotalAmount());
    Assert.assertEquals(new BigDecimal("-20"), result.getDatas().get(0).getUnSettleAmount());
  }

  /**
   * 直接结算读取源单必须走带数据权限的查询，并统一拒绝不可见 ID。
   */
  @Test
  public void directApprovePassShouldUsePermissionAwareSourceQueries() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    CustomerSettleSheetDetailService detailService = getField(service,
        "customerSettleSheetDetailService");
    SaleOutSheet sheet = saleOutSheet("sale-1", "customer-1", "10", "0",
        SettleStatus.UN_SETTLE);
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class)))
        .thenReturn(Collections.singletonList(sheet));
    Mockito.when(saleOutSheetService.setSettled("sale-1", SettleStatus.UN_SETTLE, 0L))
        .thenReturn(1);
    Mockito.when(detailService.saveBatch(Mockito.anyCollection())).thenReturn(true);

    service.directApprovePass(directSettleVo("customer-1", "10", item("sale-1", 1)));

    ArgumentCaptor<QuerySaleOutSheetVo> captor = ArgumentCaptor.forClass(
        QuerySaleOutSheetVo.class);
    Mockito.verify(saleOutSheetService).query(captor.capture());
    Assert.assertEquals(Collections.singletonList("sale-1"), captor.getValue().getIdList());
    Assert.assertNull(captor.getValue().getStatus());
    Assert.assertEquals(Boolean.TRUE, captor.getValue().getRequireTxIdNull());
    Mockito.verify(saleOutSheetService, Mockito.never()).listByIds(Mockito.anyCollection());
  }

  /**
   * 数据权限查询未返回请求 ID 时，必须按不存在或无权访问统一拒绝。
   */
  @Test
  public void directApprovePassShouldRejectInvisibleOrMissingSourceId() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class)))
        .thenReturn(Collections.emptyList());

    try {
      service.directApprovePass(directSettleVo("customer-1", "10", item("hidden-id", 1)));
      Assert.fail("不可见或不存在的源单 ID 应被拒绝");
    } catch (DefaultClientException e) {
      Assert.assertEquals("业务单据不存在或无权访问！", e.getMessage());
    }
  }

  /**
   * 未审核销售源单也允许直接结算。
   */
  @Test
  public void directApprovePassShouldAllowUnapprovedSource() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    CustomerSettleSheetDetailService detailService = getField(service,
        "customerSettleSheetDetailService");
    SaleOutSheet sheet = saleOutSheet("sale-1", "customer-1", "10", "0",
        SettleStatus.UN_SETTLE);
    sheet.setStatus(SaleOutSheetStatus.CREATED);
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class)))
        .thenReturn(Collections.singletonList(sheet));
    Mockito.when(saleOutSheetService.setSettled("sale-1", SettleStatus.UN_SETTLE, 0L))
        .thenReturn(1);
    Mockito.when(detailService.saveBatch(Mockito.anyCollection())).thenReturn(true);

    service.directApprovePass(directSettleVo("customer-1", "10", item("sale-1", 1)));

    Mockito.verify(saleOutSheetService).setSettled("sale-1", SettleStatus.UN_SETTLE, 0L);
  }

  /**
   * 即使查询依赖异常返回已被交易占用的源单，提交层仍必须拒绝。
   */
  @Test(expected = DefaultClientException.class)
  public void directApprovePassShouldRejectOccupiedSource() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    SaleOutSheet sheet = saleOutSheet("sale-1", "customer-1", "10", "0",
        SettleStatus.UN_SETTLE);
    sheet.setTxId("tx-1");
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class)))
        .thenReturn(Collections.singletonList(sheet));

    service.directApprovePass(directSettleVo("customer-1", "10", item("sale-1", 1)));
  }

  /**
   * 销售退货可按负金额直接退款并保存负向结算明细。
   */
  @Test
  public void directApprovePassShouldSettleSaleReturnAsNegativeRefund() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleReturnService saleReturnService = getField(service, "saleReturnService");
    CustomerSettleSheetDetailService detailService = getField(service,
        "customerSettleSheetDetailService");
    Mockito.when(saleReturnService.query(Mockito.any(QuerySaleReturnVo.class)))
        .thenReturn(Collections.singletonList(
            saleReturn("return-1", "customer-1", "20", SettleStatus.UN_SETTLE)));
    Mockito.when(saleReturnService.setSettled("return-1", SettleStatus.UN_SETTLE, 0L))
        .thenReturn(1);
    Mockito.when(detailService.saveBatch(Mockito.anyCollection())).thenReturn(true);

    service.directApprovePass(directSettleVo("customer-1", "-20", item("return-1", 2)));

    ArgumentCaptor<Collection<CustomerSettleSheetDetail>> detailCaptor =
        ArgumentCaptor.forClass(Collection.class);
    Mockito.verify(detailService).saveBatch(detailCaptor.capture());
    Assert.assertEquals(new BigDecimal("-20.00"),
        detailCaptor.getValue().iterator().next().getPayAmount());
  }

  /**
   * 销售单一百与退货二十混合结算时，净额必须为八十且保留各自方向。
   */
  @Test
  public void directApprovePassShouldAllocateMixedSaleAndReturnToNetAmount() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    SaleReturnService saleReturnService = getField(service, "saleReturnService");
    CustomerSettleSheetDetailService detailService = getField(service,
        "customerSettleSheetDetailService");
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class)))
        .thenReturn(Collections.singletonList(
            saleOutSheet("sale-1", "customer-1", "100", "0", SettleStatus.UN_SETTLE)));
    Mockito.when(saleReturnService.query(Mockito.any(QuerySaleReturnVo.class)))
        .thenReturn(Collections.singletonList(
            saleReturn("return-1", "customer-1", "20", SettleStatus.UN_SETTLE)));
    Mockito.when(saleOutSheetService.setSettled("sale-1", SettleStatus.UN_SETTLE, 0L))
        .thenReturn(1);
    Mockito.when(saleReturnService.setSettled("return-1", SettleStatus.UN_SETTLE, 0L))
        .thenReturn(1);
    Mockito.when(detailService.saveBatch(Mockito.anyCollection())).thenReturn(true);

    service.directApprovePass(directSettleVo("customer-1", "80", item("sale-1", 1),
        item("return-1", 2)));

    ArgumentCaptor<Collection<CustomerSettleSheetDetail>> detailCaptor =
        ArgumentCaptor.forClass(Collection.class);
    Mockito.verify(detailService).saveBatch(detailCaptor.capture());
    List<CustomerSettleSheetDetail> details = Arrays.asList(
        detailCaptor.getValue().toArray(new CustomerSettleSheetDetail[0]));
    Assert.assertEquals(new BigDecimal("100.00"), details.get(0).getPayAmount());
    Assert.assertEquals(new BigDecimal("-20.00"), details.get(1).getPayAmount());
    Assert.assertEquals(new BigDecimal("80.00"), details.stream()
        .map(CustomerSettleSheetDetail::getPayAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  /**
   * 直审必须写入审批人、客户结算操作日志契约及每张销售源单时间线。
   */
  @Test
  public void directApprovePassShouldWriteApprovalAndLogContracts() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    CustomerSettleSheetDetailService detailService = getField(service,
        "customerSettleSheetDetailService");
    CustomerSettleSheetMapper mapper = getField(service, "baseMapper");
    OrderTimeLineService orderTimeLineService = Mockito.mock(OrderTimeLineService.class);
    injectField(service, "orderTimeLineService", orderTimeLineService);
    injectField(service, "receiveOrderTimeLineBizType", new ReceiveOrderTimeLineBizType());
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class)))
        .thenReturn(Collections.singletonList(
            saleOutSheet("sale-1", "customer-1", "10", "0", SettleStatus.UN_SETTLE)));
    Mockito.when(saleOutSheetService.setSettled("sale-1", SettleStatus.UN_SETTLE, 0L))
        .thenReturn(1);
    Mockito.when(detailService.saveBatch(Mockito.anyCollection())).thenReturn(true);

    service.directApprovePass(directSettleVo("customer-1", "10", item("sale-1", 1)));

    ArgumentCaptor<CustomerSettleSheet> sheetCaptor = ArgumentCaptor.forClass(
        CustomerSettleSheet.class);
    Mockito.verify(mapper).insert(sheetCaptor.capture());
    Assert.assertEquals("user-1", sheetCaptor.getValue().getApproveBy());
    Mockito.verify(orderTimeLineService).saveBatch(Mockito.argThat(
        lines -> lines.size() == 1
            && lines.stream().allMatch(line -> "sale-1".equals(line.getOrderId())
                && line.getContent().contains("确认结算")
                && line.getContent().contains("10.00"))));
    Mockito.verify(orderTimeLineService, Mockito.never()).save(Mockito.any(OrderTimeLine.class));
    Assert.assertNotNull(CustomerSettleSheetServiceImpl.class
        .getMethod("directApprovePass", CreateCustomerSettleSheetVo.class)
        .getAnnotation(OpLog.class));
    Assert.assertNotNull(CustomerSettleSheetServiceImpl.class
        .getMethod("directApprovePass", CreateCustomerSettleSheetVo.class)
        .getAnnotation(OrderTimeLineLog.class));
  }

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
    sheet.setSettleVersion(0L);
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class))).thenReturn(
        Collections.singletonList(sheet));
    Mockito.when(saleOutSheetService.setPartSettle(Mockito.eq("sale-1"),
        Mockito.eq(SettleStatus.UN_SETTLE), Mockito.eq(0L))).thenReturn(1, 0);
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
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class))).thenReturn(
        Collections.singletonList(sheet));
    Mockito.when(saleOutSheetService.setPartSettle(Mockito.eq("sale-1"),
        Mockito.eq(SettleStatus.UN_SETTLE), Mockito.eq(0L))).thenReturn(1);
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
   * 详情查询应批量从销售源单补齐业务类型，供前端跳转到对应列表。
   */
  @Test
  public void getDetailShouldFillBizTypesFromSaleSources() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    CustomerSettleSheetMapper mapper = getField(service, "baseMapper");
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    SaleReturnService saleReturnService = getField(service, "saleReturnService");
    CustomerSettleSheetFullDto detail = new CustomerSettleSheetFullDto();
    CustomerSettleSheetFullDto.SheetDetailDto saleOutItem =
        new CustomerSettleSheetFullDto.SheetDetailDto();
    saleOutItem.setBizId("sale-out-1");
    CustomerSettleSheetFullDto.SheetDetailDto saleReturnItem =
        new CustomerSettleSheetFullDto.SheetDetailDto();
    saleReturnItem.setBizId("sale-return-1");
    detail.setDetails(Arrays.asList(saleOutItem, saleReturnItem));
    Mockito.when(mapper.getDetail("sheet-1")).thenReturn(detail);
    Mockito.when(saleOutSheetService.listByIds(Mockito.anyCollection())).thenReturn(
        Collections.singletonList(saleOutSheet("sale-out-1", "customer-1", "10", "0",
            SettleStatus.UN_SETTLE)));
    SaleReturn saleReturn = new SaleReturn();
    saleReturn.setId("sale-return-1");
    saleReturn.setCode("sale-return-1");
    Mockito.when(saleReturnService.listByIds(Mockito.anyCollection())).thenReturn(
        Collections.singletonList(saleReturn));

    CustomerSettleSheetFullDto result = service.getDetail("sheet-1");

    Assert.assertEquals(Integer.valueOf(1), result.getDetails().get(0).getBizType());
    Assert.assertEquals(Integer.valueOf(2), result.getDetails().get(1).getBizType());
    Assert.assertEquals(Integer.valueOf(1),
        new GetCustomerSettleSheetBo.SheetDetailBo(result.getDetails().get(0)).getBizType());
    Assert.assertEquals(Integer.valueOf(2),
        new GetCustomerSettleSheetBo.SheetDetailBo(result.getDetails().get(1)).getBizType());
  }

  /**
   * 直接结算时不允许混入其他客户的销售单。
   */
  @Test(expected = DefaultClientException.class)
  public void directApprovePassShouldRejectDifferentCustomers() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class))).thenReturn(
        Collections.singletonList(saleOutSheet("sale-1", "customer-2", "10", "0",
            SettleStatus.UN_SETTLE)));

    service.directApprovePass(directSettleVo("customer-1", "10", item("sale-1", 1)));
  }

  /**
   * 销售出库结算金额不能使用退款负方向。
   */
  @Test(expected = DefaultClientException.class)
  public void directApprovePassShouldRejectNegativeSettleAmount() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class))).thenReturn(
        Collections.singletonList(saleOutSheet("sale-1", "customer-1", "10", "0",
            SettleStatus.UN_SETTLE)));

    service.directApprovePass(directSettleVo("customer-1", "-0.01", item("sale-1", 1)));
  }

  /**
   * 已结算业务单据不能再次直接结算。
   */
  @Test(expected = DefaultClientException.class)
  public void directApprovePassShouldRejectSettledBizItem() throws Exception {
    CustomerSettleSheetServiceImpl service = directSettleService();
    SaleOutSheetService saleOutSheetService = getField(service, "saleOutSheetService");
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class))).thenReturn(
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
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class))).thenReturn(
        Collections.singletonList(saleOutSheet("sale-1", "customer-1", "10", "0",
            SettleStatus.UN_SETTLE)));
    Mockito.when(saleOutSheetService.setSettled(Mockito.eq("sale-1"),
        Mockito.eq(SettleStatus.UN_SETTLE), Mockito.eq(0L))).thenReturn(1);
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
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class))).thenReturn(
        Collections.singletonList(saleOutSheet("sale-1", "customer-1", "10", "0",
            SettleStatus.UN_SETTLE)));
    Mockito.when(saleOutSheetService.setSettled(Mockito.eq("sale-1"),
        Mockito.eq(SettleStatus.UN_SETTLE), Mockito.eq(0L))).thenReturn(0);

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
    Mockito.when(saleOutSheetService.query(Mockito.any(QuerySaleOutSheetVo.class))).thenReturn(
        Arrays.asList(
        saleOutSheet("sale-1", "customer-1", "10", "0", SettleStatus.UN_SETTLE),
        saleOutSheet("sale-2", "customer-1", "20", "0", SettleStatus.UN_SETTLE)));
    Mockito.when(saleOutSheetService.setPartSettle(Mockito.anyString(),
        Mockito.eq(SettleStatus.UN_SETTLE), Mockito.eq(0L))).thenReturn(1);
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
    DefaultUserDetails currentUser = new DefaultUserDetails();
    currentUser.setId("user-1");
    currentUser.setName("测试用户");
    SecurityUtil.setCurrentUser(currentUser);
    injectField(service, "saleOutSheetService", Mockito.mock(SaleOutSheetService.class));
    injectField(service, "saleReturnService", Mockito.mock(SaleReturnService.class));
    injectField(service, "customerSettleSheetDetailService", Mockito.mock(
        CustomerSettleSheetDetailService.class));
    injectField(service, "customerService", Mockito.mock(CustomerService.class));
    injectField(service, "orderTimeLineService", Mockito.mock(OrderTimeLineService.class));
    injectField(service, "receiveOrderTimeLineBizType", new ReceiveOrderTimeLineBizType());
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
    sheet.setStatus(SaleOutSheetStatus.APPROVE_PASS);
    sheet.setSettleStatus(settleStatus);
    sheet.setSettleVersion(0L);
    return sheet;
  }

  /**
   * 创建销售退货单测试数据。
   */
  private SaleReturn saleReturn(String id, String customerId, String totalAmount,
      SettleStatus settleStatus) {
    SaleReturn sheet = new SaleReturn();
    sheet.setId(id);
    sheet.setCode(id);
    sheet.setCustomerId(customerId);
    sheet.setTotalAmount(new BigDecimal(totalAmount));
    sheet.setStatus(SaleReturnStatus.APPROVE_PASS);
    sheet.setSettleStatus(settleStatus);
    sheet.setSettleVersion(0L);
    return sheet;
  }

  /**
   * 销售源单 Mapper 必须以状态和数值版本作为原子结算更新条件。
   */
  @Test
  public void saleSourceMappersShouldAtomicallyIncrementSettleVersion() throws Exception {
    String saleOutMapper = new String(Files.readAllBytes(Paths.get("..", "xingyun-sc", "src",
        "main", "resources", "mappers", "sale", "SaleOutSheetMapper.xml")),
        StandardCharsets.UTF_8);
    String saleReturnMapper = new String(Files.readAllBytes(Paths.get("..", "xingyun-sc", "src",
        "main", "resources", "mappers", "sale", "SaleReturnMapper.xml")),
        StandardCharsets.UTF_8);

    assertVersionUpdateContract(saleOutMapper);
    assertVersionUpdateContract(saleReturnMapper);
    assertCustomerSettleSourceQueryContract(saleOutMapper);
    assertCustomerSettleSourceQueryContract(saleReturnMapper);
  }

  /**
   * 旧结算入口也必须转发到带版本号的原子更新，避免绕过直接结算的并发保护。
   */
  @Test
  public void legacySaleSourceStatusUpdatesShouldUseVersionedMapperContract() throws Exception {
    String saleOutService = readScSource("SaleOutSheetServiceImpl.java");
    String saleReturnService = readScSource("SaleReturnServiceImpl.java");

    assertLegacyStatusUpdateContract(saleOutService);
    assertLegacyStatusUpdateContract(saleReturnService);
  }

  /**
   * 验证 Mapper SQL 使用结算状态和数值版本作为更新条件，并递增版本。
   */
  private void assertVersionUpdateContract(String mapperXml) {
    String updateSql = mapperXml.substring(mapperXml.indexOf(
        "id=\"updateSettleStatusWithVersion\""));
    Assert.assertTrue(updateSql.contains("settle_version = settle_version + 1"));
    Assert.assertTrue(updateSql.contains("WHERE id = #{id}"));
    Assert.assertTrue(updateSql.contains("settle_status = #{expectedStatus}"));
    Assert.assertTrue(updateSql.contains("settle_version = #{settleVersion}"));
    Assert.assertFalse(updateSql.contains("status = 3"));
    Assert.assertTrue(updateSql.contains("tx_id IS NULL"));
  }

  /**
   * 验证客户结算源单专用查询不限制审核状态，但仍排除被交易占用的单据。
   */
  private void assertCustomerSettleSourceQueryContract(String mapperXml) {
    int start = mapperXml.indexOf("<select id=\"getApprovedList\"");
    int end = mapperXml.indexOf("</select>", start);
    String querySql = mapperXml.substring(start, end);
    Assert.assertFalse(querySql.contains("status = 3"));
    Assert.assertTrue(querySql.contains("tx_id IS NULL"));
  }

  /**
   * 验证无版本参数的历史状态更新也会读取当前版本并调用版本化 Mapper。
   */
  private void assertLegacyStatusUpdateContract(String serviceSource) {
    Assert.assertTrue(serviceSource.contains(
        "updateSettleStatus(id, SettleStatus.UN_SETTLE, SettleStatus.PART_SETTLE)"));
    Assert.assertTrue(serviceSource.contains(
        "updateSettleStatus(id, SettleStatus.PART_SETTLE, SettleStatus.UN_SETTLE,"));
    Assert.assertTrue(serviceSource.contains("getBaseMapper().updateSettleStatusWithVersion(id,"));
    Assert.assertTrue(serviceSource.contains(
        "sheet.getSettleVersion() == null ? 0L : sheet.getSettleVersion()"));
  }

  /**
   * 读取销售模块服务源码。
   */
  private String readScSource(String fileName) throws Exception {
    return new String(Files.readAllBytes(Paths.get("..", "xingyun-sc", "src", "main", "java",
        "com", "lframework", "xingyun", "sc", "impl", "sale", fileName)),
        StandardCharsets.UTF_8);
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
