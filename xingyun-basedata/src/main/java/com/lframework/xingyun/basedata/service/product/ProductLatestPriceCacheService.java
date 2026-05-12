package com.lframework.xingyun.basedata.service.product;

import java.math.BigDecimal;

public interface ProductLatestPriceCacheService {

  BigDecimal getLatestSalePrice(String productId);

  BigDecimal getLatestPurchasePrice(String productId);

  void updateLatestPrice(String productId, BigDecimal latestSalePrice, BigDecimal latestPurchasePrice);
}
