package com.lframework.xingyun.basedata.impl.print;

import com.lframework.xingyun.basedata.bo.print.PrintTemplateColumnDescription;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 打印模板字段说明服务测试。
 */
public class PrintTemplateServiceImplTest {

  /**
   * 验证采购收货单业务类型返回采购入库字段。
   */
  @Test
  public void shouldReturnReceiveSheetFieldsForPurchaseBizType() {
    List<String> names = getFieldNames("2");

    Assert.assertTrue(names.contains("supplierCode"));
    Assert.assertTrue(names.contains("purchaserName"));
    Assert.assertTrue(names.contains("purchaseOrderCode"));
    Assert.assertTrue(names.contains("details[].receiveNum"));
    Assert.assertTrue(names.contains("details[].productionDate"));
  }

  /**
   * 验证销售出库业务类型继续返回销售出库字段。
   */
  @Test
  public void shouldReturnSaleOutFieldsForSaleBizType() {
    List<String> names = getFieldNames("7");

    Assert.assertTrue(names.contains("customerName"));
    Assert.assertTrue(names.contains("details[].orderNum"));
  }

  /**
   * 验证未传业务类型时保持销售出库字段兼容行为。
   */
  @Test
  public void shouldReturnSaleFieldsForBlankBizType() {
    List<String> names = getFieldNames(null);

    Assert.assertTrue(names.contains("customerName"));
  }

  /**
   * 调用待实现的业务类型字段说明方法。
   *
   * @param bizType 业务类型
   * @return 字段名称
   */
  @SuppressWarnings("unchecked")
  private List<String> getFieldNames(String bizType) {
    try {
      Method method = PrintTemplateServiceImpl.class.getMethod("getFieldDesc", String.class);
      List<PrintTemplateColumnDescription> fields = (List<PrintTemplateColumnDescription>) method
          .invoke(new PrintTemplateServiceImpl(), bizType);
      return fields.stream().map(PrintTemplateColumnDescription::getColumnName)
          .collect(Collectors.toList());
    } catch (NoSuchMethodException e) {
      throw new AssertionError("打印模板服务尚未提供按业务类型查询字段的方法", e);
    } catch (IllegalAccessException e) {
      throw new AssertionError("无法访问打印模板字段说明方法", e);
    } catch (InvocationTargetException e) {
      throw new AssertionError("打印模板字段说明方法执行失败", e.getCause());
    }
  }
}
