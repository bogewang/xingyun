package com.lframework.xingyun.settle.impl;

import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.settle.bo.sheet.customer.CustomerSaleSettleInfoBo;
import com.lframework.xingyun.settle.entity.CustomerSettleSheet;
import com.lframework.xingyun.settle.entity.CustomerSettleSheetDetail;
import com.lframework.xingyun.settle.enums.CustomerSettleSheetStatus;
import com.lframework.xingyun.settle.mappers.CustomerSettleSheetMapper;
import com.lframework.xingyun.settle.service.CustomerSettleSheetDetailService;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSaleSettleInfoVo;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * 客户结算工作台服务测试。
 */
public class CustomerSettleSheetServiceImplTest {

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
}
