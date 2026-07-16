package com.lframework.xingyun.sc.impl.product;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.xingyun.basedata.service.product.ProductReferenceChecker;
import com.lframework.xingyun.sc.entity.ProductStock;
import com.lframework.xingyun.sc.entity.ProductStockLog;
import com.lframework.xingyun.sc.entity.PurchaseOrderDetail;
import com.lframework.xingyun.sc.entity.ReceiveSheetDetail;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import com.lframework.xingyun.sc.mappers.ProductStockLogMapper;
import com.lframework.xingyun.sc.mappers.ProductStockMapper;
import com.lframework.xingyun.sc.mappers.PurchaseOrderDetailMapper;
import com.lframework.xingyun.sc.mappers.ReceiveSheetDetailMapper;
import com.lframework.xingyun.sc.mappers.SaleOutSheetDetailMapper;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 仓库业务中的商品引用检查器。
 */
@Service
public class ScProductReferenceChecker implements ProductReferenceChecker {

  private final PurchaseOrderDetailMapper purchaseOrderDetailMapper;
  private final ReceiveSheetDetailMapper receiveSheetDetailMapper;
  private final SaleOutSheetDetailMapper saleOutSheetDetailMapper;
  private final ProductStockMapper productStockMapper;
  private final ProductStockLogMapper productStockLogMapper;

  /**
   * 创建仓库业务商品引用检查器。
   *
   * @param purchaseOrderDetailMapper 采购订单明细 Mapper
   * @param receiveSheetDetailMapper 收货单明细 Mapper
   * @param saleOutSheetDetailMapper 销售出库明细 Mapper
   * @param productStockMapper 商品库存 Mapper
   * @param productStockLogMapper 商品库存流水 Mapper
   */
  public ScProductReferenceChecker(PurchaseOrderDetailMapper purchaseOrderDetailMapper,
      ReceiveSheetDetailMapper receiveSheetDetailMapper,
      SaleOutSheetDetailMapper saleOutSheetDetailMapper, ProductStockMapper productStockMapper,
      ProductStockLogMapper productStockLogMapper) {
    this.purchaseOrderDetailMapper = purchaseOrderDetailMapper;
    this.receiveSheetDetailMapper = receiveSheetDetailMapper;
    this.saleOutSheetDetailMapper = saleOutSheetDetailMapper;
    this.productStockMapper = productStockMapper;
    this.productStockLogMapper = productStockLogMapper;
  }

  /**
   * 判断任一引用计数是否大于零。
   *
   * @param referenceCounts 各业务数据的引用计数
   * @return 任一计数大于零时返回 true
   */
  static boolean hasReference(List<Long> referenceCounts) {
    return referenceCounts.stream().anyMatch(count -> count > 0L);
  }

  /**
   * 判断商品是否被采购、入库、销售出库或库存数据引用。
   *
   * @param productId 商品 ID
   * @return 已被引用时返回 true
   */
  @Override
  public boolean hasReference(String productId) {
    return hasReference(Arrays.asList(
        purchaseOrderDetailMapper.selectCount(Wrappers.lambdaQuery(PurchaseOrderDetail.class)
            .eq(PurchaseOrderDetail::getProductId, productId)).longValue(),
        receiveSheetDetailMapper.selectCount(Wrappers.lambdaQuery(ReceiveSheetDetail.class)
            .eq(ReceiveSheetDetail::getProductId, productId)).longValue(),
        saleOutSheetDetailMapper.selectCount(Wrappers.lambdaQuery(SaleOutSheetDetail.class)
            .eq(SaleOutSheetDetail::getProductId, productId)).longValue(),
        productStockMapper.selectCount(Wrappers.lambdaQuery(ProductStock.class)
            .eq(ProductStock::getProductId, productId)).longValue(),
        productStockLogMapper.selectCount(Wrappers.lambdaQuery(ProductStockLog.class)
            .eq(ProductStockLog::getProductId, productId)).longValue()));
  }
}
