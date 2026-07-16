package com.lframework.xingyun.sc.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.xingyun.core.service.ProductDeleteReferenceChecker;
import com.lframework.xingyun.sc.entity.PurchaseOrderDetail;
import com.lframework.xingyun.sc.entity.ReceiveSheetDetail;
import com.lframework.xingyun.sc.entity.SaleOrderDetail;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import com.lframework.xingyun.sc.mappers.PurchaseOrderDetailMapper;
import com.lframework.xingyun.sc.mappers.ReceiveSheetDetailMapper;
import com.lframework.xingyun.sc.mappers.SaleOrderDetailMapper;
import com.lframework.xingyun.sc.mappers.SaleOutSheetDetailMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 商品删除前的仓库业务单据引用检查器。
 */
@Service
public class ProductDeleteReferenceCheckerImpl implements ProductDeleteReferenceChecker {

  private final PurchaseOrderDetailMapper purchaseOrderDetailMapper;
  private final ReceiveSheetDetailMapper receiveSheetDetailMapper;
  private final SaleOrderDetailMapper saleOrderDetailMapper;
  private final SaleOutSheetDetailMapper saleOutSheetDetailMapper;

  /**
   * 创建商品引用检查器。
   *
   * @param purchaseOrderDetailMapper 采购订单明细 Mapper
   * @param receiveSheetDetailMapper 采购收货单明细 Mapper
   * @param saleOrderDetailMapper 销售订单明细 Mapper
   * @param saleOutSheetDetailMapper 销售出库单明细 Mapper
   */
  @Autowired
  public ProductDeleteReferenceCheckerImpl(PurchaseOrderDetailMapper purchaseOrderDetailMapper,
      ReceiveSheetDetailMapper receiveSheetDetailMapper, SaleOrderDetailMapper saleOrderDetailMapper,
      SaleOutSheetDetailMapper saleOutSheetDetailMapper) {
    this.purchaseOrderDetailMapper = purchaseOrderDetailMapper;
    this.receiveSheetDetailMapper = receiveSheetDetailMapper;
    this.saleOrderDetailMapper = saleOrderDetailMapper;
    this.saleOutSheetDetailMapper = saleOutSheetDetailMapper;
  }

  /**
   * 判断商品是否被任一仓库业务单据明细引用。
   *
   * @param productId 商品 ID
   * @return 已引用时返回 true
   */
  @Override
  public boolean isReferenced(String productId) {
    return purchaseOrderDetailMapper.selectCount(Wrappers.lambdaQuery(PurchaseOrderDetail.class)
        .eq(PurchaseOrderDetail::getProductId, productId)) > 0
        || receiveSheetDetailMapper.selectCount(Wrappers.lambdaQuery(ReceiveSheetDetail.class)
        .eq(ReceiveSheetDetail::getProductId, productId)) > 0
        || saleOrderDetailMapper.selectCount(Wrappers.lambdaQuery(SaleOrderDetail.class)
        .eq(SaleOrderDetail::getProductId, productId)) > 0
        || saleOutSheetDetailMapper.selectCount(Wrappers.lambdaQuery(SaleOutSheetDetail.class)
        .eq(SaleOutSheetDetail::getProductId, productId)) > 0;
  }
}
