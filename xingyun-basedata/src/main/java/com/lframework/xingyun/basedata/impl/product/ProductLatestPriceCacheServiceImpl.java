package com.lframework.xingyun.basedata.impl.product;

import com.lframework.starter.web.core.components.redis.RedisHandler;
import com.lframework.starter.web.core.components.tenant.TenantContextHolder;
import com.lframework.xingyun.basedata.service.product.ProductLatestPriceCacheService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.math.BigDecimal;

@Service
public class ProductLatestPriceCacheServiceImpl implements ProductLatestPriceCacheService {

    private static final String CACHE_NAME = "product_latest_price";

    @Autowired
    private RedisHandler redisHandler;

    @Override
    public BigDecimal getLatestSalePrice(String productId) {

        ProductLatestPriceCacheItem item = this.getCacheItem(productId);
        return item == null ? null : item.getLatestSalePrice();
    }

    @Override
    public BigDecimal getLatestPurchasePrice(String productId) {

        ProductLatestPriceCacheItem item = this.getCacheItem(productId);
        return item == null ? null : item.getLatestPurchasePrice();
    }

    @Override
    public void updateLatestPrice(String productId, BigDecimal latestSalePrice,
                                  BigDecimal latestPurchasePrice) {

        if (productId == null || (latestSalePrice == null && latestPurchasePrice == null)) {
            return;
        }

        ProductLatestPriceCacheItem item = this.getCacheItem(productId);
        if (item == null) {
            item = new ProductLatestPriceCacheItem();
        }

        if (latestSalePrice != null) {
            item.setLatestSalePrice(latestSalePrice);
        }
        if (latestPurchasePrice != null) {
            item.setLatestPurchasePrice(latestPurchasePrice);
        }

        redisHandler.set(this.buildKey(productId), item, -1L);
    }

    private ProductLatestPriceCacheItem getCacheItem(String productId) {
        return (ProductLatestPriceCacheItem) redisHandler.get(this.buildKey(productId));
    }

    private String buildKey(String productId) {

        return TenantContextHolder.getTenantId() + ":" + CACHE_NAME + ":" + productId;
    }

    @Data
    public static class ProductLatestPriceCacheItem implements Serializable {

        private static final long serialVersionUID = 1L;

        private BigDecimal latestSalePrice;

        private BigDecimal latestPurchasePrice;
    }
}
