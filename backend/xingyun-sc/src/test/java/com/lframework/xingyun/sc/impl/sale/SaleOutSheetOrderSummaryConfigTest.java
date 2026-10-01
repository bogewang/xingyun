package com.lframework.xingyun.sc.impl.sale;

import com.lframework.starter.web.inner.entity.SysParameter;
import com.lframework.starter.web.inner.service.system.SysParameterService;
import com.lframework.starter.web.inner.vo.system.parameter.QuerySysParameterVo;
import java.lang.reflect.Field;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证订单汇总按钮系统参数及默认行为。 */
class SaleOutSheetOrderSummaryConfigTest {

  /** 参数为真显示、为假隐藏，未配置时兼容原有显示行为。 */
  @Test
  void shouldReadVisibilityParameter() throws Exception {
    SysParameterService parameters = mock(SysParameterService.class);
    SaleOutSheetServiceImpl service = new SaleOutSheetServiceImpl();
    Field field = SaleOutSheetServiceImpl.class.getDeclaredField("sysParameterService");
    field.setAccessible(true);
    field.set(service, parameters);
    when(parameters.query(any(QuerySysParameterVo.class))).thenReturn(Collections.emptyList());
    assertTrue(service.getOrderSummaryJsExportConfig());
    ArgumentCaptor<QuerySysParameterVo> query = ArgumentCaptor.forClass(QuerySysParameterVo.class);
    verify(parameters).query(query.capture());
    assertEquals("sale_out_order_summary_export_enabled", query.getValue().getPmKey());

    SysParameter parameter = new SysParameter();
    parameter.setPmValue("false");
    when(parameters.query(any(QuerySysParameterVo.class)))
        .thenReturn(Collections.singletonList(parameter));
    assertFalse(service.getOrderSummaryJsExportConfig());
    parameter.setPmValue("true");
    assertTrue(service.getOrderSummaryJsExportConfig());
  }
}
