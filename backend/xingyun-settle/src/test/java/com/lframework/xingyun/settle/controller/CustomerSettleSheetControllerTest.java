package com.lframework.xingyun.settle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lframework.starter.web.core.annotations.security.HasPermission;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.InvokeResultBuilder;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.components.trace.TraceBuilder;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.starter.web.core.components.security.UserTokenResolver;
import com.lframework.starter.mq.core.service.MqProducerService;
import com.lframework.xingyun.settle.bo.sheet.customer.CustomerSaleSettleInfoBo;
import com.lframework.xingyun.settle.entity.CustomerSettleSheet;
import com.lframework.xingyun.settle.excel.sheet.customer.CustomerSaleSettleInfoExportTaskWorker;
import com.lframework.xingyun.settle.excel.sheet.customer.CustomerSettleSheetExportTaskWorker;
import com.lframework.xingyun.settle.service.CustomerSettleSheetService;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSettleSheetVo;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSaleSettleInfoVo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 客户结算单控制器测试。
 */
public class CustomerSettleSheetControllerTest {

  /**
   * 导出客户销售结算工作台时，应成功创建导出任务。
   */
  @Test
  public void shouldCreateCustomerWorkbenchExportTask() {
    MqProducerService mqProducerService = initializeApplicationContext();
    CustomerSettleSheetController controller = new CustomerSettleSheetController();

    InvokeResult<Void> response = controller.exportSaleSettleInfos(
        new QueryCustomerSaleSettleInfoVo());

    Assert.assertEquals(InvokeResultBuilder.success().getCode(), response.getCode());
    Mockito.verify(mqProducerService).addExportTask(Mockito.argThat(task ->
        CustomerSaleSettleInfoExportTaskWorker.class.getName().equals(task.getReqClassName())));
  }

  /**
   * 导出客户结算记录时，应创建使用客户结算单数据的导出任务。
   */
  @Test
  public void shouldCreateCustomerSettleRecordExportTask() {
    MqProducerService mqProducerService = initializeApplicationContext();
    CustomerSettleSheetController controller = new CustomerSettleSheetController();

    InvokeResult<Void> response = controller.exportRecord(new QueryCustomerSettleSheetVo());

    Assert.assertEquals(InvokeResultBuilder.success().getCode(), response.getCode());
    Mockito.verify(mqProducerService).addExportTask(Mockito.argThat(task ->
        CustomerSettleSheetExportTaskWorker.class.getName().equals(task.getReqClassName())));
  }

  /**
   * 两个导出接口应使用 JSON 请求体接收与查询接口一致的筛选条件。
   */
  @Test
  public void shouldReceiveExportFiltersFromRequestBody() throws Exception {
    Method exportSaleSettleInfos = CustomerSettleSheetController.class.getDeclaredMethod(
        "exportSaleSettleInfos", QueryCustomerSaleSettleInfoVo.class);
    Method exportRecord = CustomerSettleSheetController.class.getDeclaredMethod("exportRecord",
        QueryCustomerSettleSheetVo.class);

    Assert.assertTrue(exportSaleSettleInfos.getParameters()[0].isAnnotationPresent(
        RequestBody.class));
    Assert.assertTrue(exportRecord.getParameters()[0].isAnnotationPresent(RequestBody.class));
  }

  /**
   * 工作台导出任务应委托新的客户销售结算工作台查询。
   */
  @Test
  public void shouldQuerySaleSettleInfosWhenExportingWorkbench() {
    CustomerSettleSheetService customerSettleSheetService = Mockito.mock(
        CustomerSettleSheetService.class);
    initializeServiceApplicationContext(customerSettleSheetService);
    QueryCustomerSaleSettleInfoVo params = new QueryCustomerSaleSettleInfoVo();
    params.setBizType(1);
    PageResult<CustomerSaleSettleInfoBo> expected = PageResultUtil.newInstance(2, 50, 0,
        Collections.emptyList());
    Mockito.when(customerSettleSheetService.querySaleSettleInfos(params)).thenReturn(expected);

    PageResult<CustomerSaleSettleInfoBo> actual = new CustomerSaleSettleInfoExportTaskWorker()
        .getDataList(2, 50, params);

    Assert.assertSame(expected, actual);
    Assert.assertEquals(Integer.valueOf(2), params.getPageIndex());
    Assert.assertEquals(Integer.valueOf(50), params.getPageSize());
    Mockito.verify(customerSettleSheetService).querySaleSettleInfos(params);
  }

  /**
   * 结算记录导出任务应只委托保留的客户结算单查询。
   */
  @Test
  public void shouldQueryCustomerSettleSheetsWhenExportingRecords() {
    CustomerSettleSheetService customerSettleSheetService = Mockito.mock(
        CustomerSettleSheetService.class);
    initializeServiceApplicationContext(customerSettleSheetService);
    QueryCustomerSettleSheetVo params = new QueryCustomerSettleSheetVo();
    PageResult<CustomerSettleSheet> expected = PageResultUtil.newInstance(2, 50, 0,
        Collections.emptyList());
    Mockito.when(customerSettleSheetService.query(2, 50, params)).thenReturn(expected);

    PageResult<CustomerSettleSheet> actual = new CustomerSettleSheetExportTaskWorker().getDataList(
        2, 50, params);

    Assert.assertSame(expected, actual);
    Mockito.verify(customerSettleSheetService).query(2, 50, params);
  }

  /**
   * 查询客户销售结算工作台时，应保留服务返回的分页信息。
   */
  @Test
  public void shouldKeepSaleSettleInfoPageWhenQuerySaleSettleInfos() throws Exception {
    initializeApplicationContext();
    CustomerSettleSheetService customerSettleSheetService = Mockito.mock(
        CustomerSettleSheetService.class);
    CustomerSettleSheetController controller = new CustomerSettleSheetController();
    injectField(controller, "customerSettleSheetService", customerSettleSheetService);

    QueryCustomerSaleSettleInfoVo vo = new QueryCustomerSaleSettleInfoVo();
    CustomerSaleSettleInfoBo settleInfo = new CustomerSaleSettleInfoBo();
    List<CustomerSaleSettleInfoBo> datas = Collections.singletonList(settleInfo);
    PageResult<CustomerSaleSettleInfoBo> expected = PageResultUtil.newInstance(1, 20, 3, datas);
    Mockito.when(customerSettleSheetService.querySaleSettleInfos(vo)).thenReturn(expected);

    InvokeResult<?> response = controller.querySaleSettleInfos(vo);

    Assert.assertSame(expected, response.getData());
  }

  /**
   * 初始化 InvokeResult 构建所需的 TraceBuilder Bean。
   */
  private MqProducerService initializeApplicationContext() {
    TraceBuilder traceBuilder = Mockito.mock(TraceBuilder.class);
    Mockito.when(traceBuilder.getTraceId(false)).thenReturn("test-trace-id");
    MqProducerService mqProducerService = Mockito.mock(MqProducerService.class);
    UserTokenResolver userTokenResolver = Mockito.mock(UserTokenResolver.class);
    Mockito.when(userTokenResolver.getToken()).thenReturn("test-token");
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    applicationContext.getBeanFactory().registerSingleton("traceBuilder", traceBuilder);
    applicationContext.getBeanFactory().registerSingleton("mqProducerService", mqProducerService);
    applicationContext.getBeanFactory().registerSingleton("userTokenResolver", userTokenResolver);
    applicationContext.getBeanFactory().registerSingleton("objectMapper", new ObjectMapper());
    applicationContext.refresh();
    new ApplicationUtil().setApplicationContext(applicationContext);
    return mqProducerService;
  }

  /**
   * 初始化导出 Worker 查询所需的客户结算单服务。
   *
   * @param customerSettleSheetService 客户结算单服务
   */
  private void initializeServiceApplicationContext(
      CustomerSettleSheetService customerSettleSheetService) {
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    applicationContext.getBeanFactory().registerSingleton("customerSettleSheetService",
        customerSettleSheetService);
    applicationContext.refresh();
    new ApplicationUtil().setApplicationContext(applicationContext);
  }

  /**
   * 向控制器注入 Mockito 创建的服务依赖。
   *
   * @param target 目标对象
   * @param fieldName 字段名称
   * @param value 注入值
   * @throws ReflectiveOperationException 反射注入失败时抛出
   */
  private void injectField(Object target, String fieldName, Object value)
      throws ReflectiveOperationException {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
