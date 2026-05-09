package com.lframework.xingyun.sc.impl.purchase;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.Assert;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.sc.entity.PurchaseOrderDetail;
import com.lframework.xingyun.sc.mappers.PurchaseOrderDetailMapper;
import com.lframework.xingyun.sc.service.purchase.PurchaseOrderDetailService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PurchaseOrderDetailServiceImpl extends
    BaseMpServiceImpl<PurchaseOrderDetailMapper, PurchaseOrderDetail>
    implements PurchaseOrderDetailService {

  @Autowired
  private ProductService productService;

  @Override
  public List<PurchaseOrderDetail> getByOrderIds(List<String> orderIds) {
    if (CollectionUtils.isEmpty(orderIds)) {
      return Lists.newArrayList();
    }
    LambdaQueryWrapper<PurchaseOrderDetail> in = Wrappers.lambdaQuery(PurchaseOrderDetail.class)
            .in(PurchaseOrderDetail::getOrderId, orderIds);
    return getBaseMapper().selectList(in);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void addReceiveNum(String id, BigDecimal num) {

    Assert.notBlank(id);
    Assert.greaterThanZero(num);

    PurchaseOrderDetail orderDetail = getBaseMapper().selectById(id);

    BigDecimal remainNum = NumberUtil.sub(orderDetail.getOrderNum(), orderDetail.getReceiveNum());
    if (NumberUtil.lt(remainNum, num)) {
      Product product = productService.findById(orderDetail.getProductId());

      throw new DefaultClientException(
          "（" + product.getCode() + "）" + product.getName() + "剩余收货数量为" + remainNum
              + "，本次收货数量不允许大于"
              + remainNum + "！");
    }

    if (getBaseMapper().addReceiveNum(orderDetail.getId(), num) != 1) {
      Product product = productService.findById(orderDetail.getProductId());

      throw new DefaultClientException(
          "（" + product.getCode() + "）" + product.getName() + "剩余收货数量不足，不允许继续收货！");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void subReceiveNum(String id, BigDecimal num) {

    Assert.notBlank(id);
    Assert.greaterThanZero(num);

    PurchaseOrderDetail orderDetail = getBaseMapper().selectById(id);

    if (NumberUtil.lt(orderDetail.getReceiveNum(), num)) {
      Product product = productService.findById(orderDetail.getProductId());

      throw new DefaultClientException(
          "（" + product.getCode() + "）" + product.getName() + "已收货数量为" + orderDetail.getReceiveNum()
              + "，本次取消收货数量不允许大于" + orderDetail.getReceiveNum() + "！");
    }

    if (getBaseMapper().subReceiveNum(orderDetail.getId(), num) != 1) {
      Product product = productService.findById(orderDetail.getProductId());

      throw new DefaultClientException(
          "（" + product.getCode() + "）" + product.getName() + "已收货数量不足，不允许取消收货！");
    }
  }
}
