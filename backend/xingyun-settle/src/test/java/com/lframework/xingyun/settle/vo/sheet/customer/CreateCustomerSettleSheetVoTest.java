package com.lframework.xingyun.settle.vo.sheet.customer;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import java.math.BigDecimal;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

/**
 * 客户结算创建参数测试。
 */
public class CreateCustomerSettleSheetVoTest {

  /**
   * 业务参数错误必须统一抛出 DefaultClientException。
   */
  @Test
  public void validateShouldUseDefaultClientException() {
    CreateCustomerSettleSheetVo vo = new CreateCustomerSettleSheetVo();
    vo.setCustomerId("customer-1");
    vo.setSettleAmount(BigDecimal.ONE);
    CustomerSettleSheetItemVo item = new CustomerSettleSheetItemVo();
    item.setBizType(1);
    vo.setItems(Collections.singletonList(item));

    try {
      vo.validate();
      Assert.fail("空业务单据 ID 应抛出业务异常");
    } catch (RuntimeException e) {
      Assert.assertEquals(DefaultClientException.class, e.getClass());
    }
  }
}
