package com.lframework.xingyun.sc.impl.sale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.inner.service.system.SysUserService;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.sc.bo.sale.out.QuerySaleOutSheetBo;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.enums.SaleOutSheetStatus;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetQueryService;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

class SaleOutSheetQueryServiceTest {

    /** 验证查询响应的每行毛利率和当前页加权汇总，包含零验收及负利润。 */
    @Test
    void shouldReturnRowAndPageProfitRates() throws Exception {
        synchronized (ApplicationUtil.class) {
            Field contextField = ApplicationUtil.class.getDeclaredField("APPLICATION_CONTEXT");
            contextField.setAccessible(true);
            ApplicationContext original = (ApplicationContext) contextField.get(null);
            ApplicationContext context = mock(ApplicationContext.class);
            CustomerService customerService = mock(CustomerService.class);
            when(customerService.findById("customer")).thenReturn(new Customer());
            when(context.getBean(CustomerService.class)).thenReturn(customerService);
            when(context.getBean(SysUserService.class)).thenReturn(mock(SysUserService.class));
            new ApplicationUtil().setApplicationContext(context);
            try {
                SaleOutSheetService source = mock(SaleOutSheetService.class);
                SaleOutSheetQueryService service = new SaleOutSheetQueryService();
                ReflectionTestUtils.setField(service, "saleOutSheetService", source);
                QuerySaleOutSheetVo vo = new QuerySaleOutSheetVo();
                PageResult<SaleOutSheet> page = new PageResult<>();
                page.setTotalCount(2);
                page.setDatas(Arrays.asList(sheet("100", "0", "-10"), sheet("200", "180", "30")));
                when(source.query(1, 20, vo)).thenReturn(page);
                PageResult<QuerySaleOutSheetBo> result = service.query(1, 20, vo);
                assertEquals(new BigDecimal("-10.00"), result.getDatas().get(0).getProfitRate());
                assertEquals(new BigDecimal("16.67"), result.getDatas().get(1).getProfitRate());
                assertEquals(new BigDecimal("7.14"), result.getExtra().get("profitRate"));
                assertEquals(2, result.getTotalCount());

                PageResult<SaleOutSheet> emptyPage = new PageResult<>();
                emptyPage.setDatas(Collections.emptyList());
                when(source.query(1, 20, vo)).thenReturn(emptyPage);
                assertEquals(new BigDecimal("0.00"), service.query(1, 20, vo).getExtra().get("profitRate"));
            } finally {
                new ApplicationUtil().setApplicationContext(original);
            }
        }
    }

    /** 构造成本尚未补全的单据，验证计算依赖总利润而非成本字段。 */
    private SaleOutSheet sheet(String amount, String confirmAmount, String profit) {
        SaleOutSheet sheet = new SaleOutSheet();
        sheet.setCustomerId("customer");
        sheet.setStatus(SaleOutSheetStatus.values()[0]);
        sheet.setSettleStatus(SettleStatus.values()[0]);
        sheet.setTotalAmount(new BigDecimal(amount));
        sheet.setConfirmAmt(new BigDecimal(confirmAmount));
        sheet.setTotalProfit(new BigDecimal(profit));
        sheet.setTotalCost(BigDecimal.ONE);
        sheet.setFillAllCost(false);
        return sheet;
    }
}
