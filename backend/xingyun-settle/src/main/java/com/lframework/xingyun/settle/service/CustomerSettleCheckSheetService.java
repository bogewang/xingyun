package com.lframework.xingyun.settle.service;

import com.lframework.starter.web.core.service.BaseMpService;
import com.lframework.xingyun.settle.entity.CustomerSettleCheckSheet;
import com.lframework.xingyun.settle.vo.check.customer.CreateCustomerSettleCheckSheetVo;

/**
 * 客户对账单服务。
 */
public interface CustomerSettleCheckSheetService extends BaseMpService<CustomerSettleCheckSheet> {

  /**
   * 直接确认客户对账单。
   *
   * @param vo 客户对账确认请求
   * @return 已确认对账单ID
   */
  String directApprovePass(CreateCustomerSettleCheckSheetVo vo);
}
