package com.lframework.xingyun.settle.controller;

import com.lframework.starter.web.core.annotations.security.HasPermission;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.components.trace.TraceBuilder;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.settle.bo.sheet.customer.CustomerSaleSettleInfoBo;
import com.lframework.xingyun.settle.service.CustomerSettleSheetService;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSaleSettleInfoVo;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.support.StaticApplicationContext;

/**
 * 客户结算单控制器测试。
 */
public class CustomerSettleSheetControllerTest {

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
  private void initializeApplicationContext() {
    TraceBuilder traceBuilder = Mockito.mock(TraceBuilder.class);
    Mockito.when(traceBuilder.getTraceId(false)).thenReturn("test-trace-id");
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    applicationContext.getBeanFactory().registerSingleton("traceBuilder", traceBuilder);
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
