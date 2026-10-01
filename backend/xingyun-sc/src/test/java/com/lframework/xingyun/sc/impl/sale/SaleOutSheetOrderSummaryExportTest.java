package com.lframework.xingyun.sc.impl.sale;

import com.lframework.xingyun.sc.dto.sale.out.QuerySaleOutSheetDetailDto;
import com.lframework.xingyun.sc.mappers.SaleOutSheetMapper;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证订单汇总导出固定按订单日期正序排列。 */
class SaleOutSheetOrderSummaryExportTest {

  /** 查询及勾选顺序倒序时，导出按日期正序排列，并保留单内商品顺序。 */
  @Test
  void shouldExportOrdersByOrderDateAscending() throws Exception {
    SaleOutSheetMapper mapper = mock(SaleOutSheetMapper.class);
    when(mapper.queryDetail(any(QuerySaleOutSheetVo.class))).thenReturn(Arrays.asList(
        detail("2", "客户乙", "白菜"), detail("1", "客户甲", "苹果"),
        detail("1", "客户甲", "香蕉")));
    SaleOutSheetServiceImpl service = new SaleOutSheetServiceImpl();
    injectMapper(service, mapper);
    QuerySaleOutSheetVo vo = new QuerySaleOutSheetVo();
    vo.setIdList(Arrays.asList("2", "1"));
    MockHttpServletResponse response = new MockHttpServletResponse();
    service.exportOrderSummary(vo, response);
    try (XSSFWorkbook workbook = new XSSFWorkbook(
        new ByteArrayInputStream(response.getContentAsByteArray()))) {
      Sheet sheet = workbook.getSheetAt(0);
      assertEquals("客户名称：客户甲", sheet.getRow(2).getCell(0).getStringCellValue());
      assertEquals("苹果", sheet.getRow(4).getCell(1).getStringCellValue());
      assertEquals("香蕉", sheet.getRow(5).getCell(1).getStringCellValue());
      assertEquals("客户名称：客户乙", sheet.getRow(12).getCell(0).getStringCellValue());
    }
    verify(mapper, times(1)).queryDetail(any(QuerySaleOutSheetVo.class));
  }

  /** 构造用于辨识单据及明细顺序的数据。 */
  private QuerySaleOutSheetDetailDto detail(String id, String customer, String product) {
    QuerySaleOutSheetDetailDto detail = new QuerySaleOutSheetDetailDto();
    detail.setId(id);
    detail.setCustomerName(customer);
    detail.setProductName(product);
    detail.setOrderDate("1".equals(id) ? "2026-09-30" : "2026-10-01");
    return detail;
  }

  /** 注入基础服务持有的 Mapper，直接验证真实导出入口。 */
  private void injectMapper(SaleOutSheetServiceImpl service, SaleOutSheetMapper mapper)
      throws Exception {
    Class<?> type = service.getClass();
    while (type != null) {
      try {
        Field field = type.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, mapper);
        return;
      } catch (NoSuchFieldException ignored) {
        type = type.getSuperclass();
      }
    }
    throw new AssertionError("未找到 BaseMapper 字段");
  }
}
