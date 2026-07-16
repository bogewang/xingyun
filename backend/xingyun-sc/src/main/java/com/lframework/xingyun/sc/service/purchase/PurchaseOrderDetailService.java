package com.lframework.xingyun.sc.service.purchase;

import com.lframework.starter.web.core.service.BaseMpService;
import com.lframework.xingyun.sc.entity.PurchaseOrderDetail;
import java.math.BigDecimal;
import java.util.List;

public interface PurchaseOrderDetailService extends BaseMpService<PurchaseOrderDetail> {

  /**
   * 根据订单ID查询
   *
   * @param orderIds
   * @return
   */
  List<PurchaseOrderDetail> getByOrderIds(List<String> orderIds);

  /**
   * 增加收货数量
   *
   * @param id
   * @param num
   */
  void addReceiveNum(String id, BigDecimal num);

  /**
   * 减少收货数量
   *
   * @param id
   * @param num
   */
  void subReceiveNum(String id, BigDecimal num);
}
