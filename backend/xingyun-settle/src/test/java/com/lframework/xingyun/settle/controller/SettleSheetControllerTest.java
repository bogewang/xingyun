package com.lframework.xingyun.settle.controller;

import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.components.trace.TraceBuilder;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.sc.entity.ReceiveSheet;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetService;
import com.lframework.xingyun.sc.vo.purchase.receive.QueryReceiveSheetVo;
import com.lframework.xingyun.settle.bo.sheet.ReceiveSheetSettleInfoBo;
import com.lframework.xingyun.settle.service.SettleSheetService;
import org.mockito.Mockito;
import org.springframework.context.support.StaticApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

/**
 * 供应商结算单控制器测试。
 */
public class SettleSheetControllerTest {

    /**
     * 查询收货单结算扩展信息时，应保留原收货单分页信息并替换为结算扩展数据。
     */
    @Test
    public void shouldKeepReceiveSheetPageWhenQuerySettleInfos() throws Exception {
        initializeApplicationContext();
        ReceiveSheetService receiveSheetService = Mockito.mock(ReceiveSheetService.class);
        SettleSheetService settleSheetService = Mockito.mock(SettleSheetService.class);
        SettleSheetController controller = new SettleSheetController();
        injectField(controller, "receiveSheetService", receiveSheetService);
        injectField(controller, "settleSheetService", settleSheetService);

        QueryReceiveSheetVo vo = new QueryReceiveSheetVo();
        vo.setPageIndex(1);
        vo.setPageSize(20);
        ReceiveSheet receiveSheet = new ReceiveSheet();
        PageResult<ReceiveSheet> receiveSheetPage = PageResultUtil.newInstance(1, 20, 3,
                Collections.singletonList(receiveSheet));
        ReceiveSheetSettleInfoBo settleInfo = new ReceiveSheetSettleInfoBo();
        List<ReceiveSheetSettleInfoBo> settleInfos = Collections.singletonList(settleInfo);
        Mockito.when(receiveSheetService.query(1, 20, vo)).thenReturn(receiveSheetPage);
        Mockito.when(settleSheetService.queryReceiveSheetSettleInfos(receiveSheetPage.getDatas()))
                .thenReturn(settleInfos);

        InvokeResult<?> response = controller.queryReceiveSheetSettleInfos(vo);

        PageResult<?> result = (PageResult<?>) response.getData();
        Assert.assertEquals(result.getTotalCount(), 3L);
        Assert.assertEquals(result.getDatas(), settleInfos);
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
