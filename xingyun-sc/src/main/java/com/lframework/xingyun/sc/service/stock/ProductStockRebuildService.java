package com.lframework.xingyun.sc.service.stock;

public interface ProductStockRebuildService {

    /**
     * 基于采购收货单和销售出库单重建库存相关数据
     */
    void rebuildByReceiveAndSaleSheets();
}
