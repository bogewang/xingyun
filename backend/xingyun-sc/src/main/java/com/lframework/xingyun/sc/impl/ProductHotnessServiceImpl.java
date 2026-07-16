package com.lframework.xingyun.sc.impl;

import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.components.redis.RedisHandler;
import com.lframework.starter.web.core.components.tenant.TenantContextHolder;
import com.lframework.xingyun.sc.service.ProductHotnessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductHotnessServiceImpl implements ProductHotnessService {

    private static final String CACHE_NAME = "product_hot_count";

    @Autowired
    private RedisHandler redisHandler;

    @Override
    public void increment(Collection<String> productIds) {

        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        List<String> validProductIds = productIds.stream()
                .filter(t -> !StringUtil.isBlank(t))
                .collect(Collectors.toList());
        if (validProductIds.isEmpty()) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    doIncrement(validProductIds);
                }
            });
            return;
        }

        doIncrement(validProductIds);
    }

    private void doIncrement(Collection<String> productIds) {

        for (String productId : productIds) {
            Long count = getCount(productId);
            redisHandler.set(buildKey(productId), count + 1, -1L);
        }
    }

    @Override
    public Map<String, Integer> getHotLevels(Collection<String> productIds) {

        Map<String, Integer> results = new HashMap<>();
        if (productIds == null || productIds.isEmpty()) {
            return results;
        }

        List<String> distinctProductIds = productIds.stream()
                .filter(t -> !StringUtil.isBlank(t))
                .distinct()
                .collect(Collectors.toList());
        if (distinctProductIds.isEmpty()) {
            return results;
        }

        Map<String, BigDecimal> counts = new LinkedHashMap<>();
        for (String productId : distinctProductIds) {
            BigDecimal count = convertCount(getCount(productId));
            if (count.compareTo(BigDecimal.ZERO) > 0) {
                counts.put(productId, count);
            }
        }

        List<Map.Entry<String, BigDecimal>> sortedCounts = new ArrayList<>(counts.entrySet());
        sortedCounts.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        BigDecimal previousCount = null;
        int level = 5;
        for (Map.Entry<String, BigDecimal> item : sortedCounts) {
            if (previousCount != null && item.getValue().compareTo(previousCount) != 0) {
                level--;
            }
            if (level <= 0) {
                break;
            }
            results.put(item.getKey(), level);
            previousCount = item.getValue();
        }

        return results;
    }

    private BigDecimal convertCount(Object count) {

        if (count == null) {
            return BigDecimal.ZERO;
        }

        if (count instanceof BigDecimal) {
            return (BigDecimal) count;
        }

        if (count instanceof Number) {
            return BigDecimal.valueOf(((Number) count).doubleValue());
        }

        return new BigDecimal(count.toString());
    }

    private Long getCount(String productId) {

        Object value = redisHandler.get(buildKey(productId));
        if (value == null) {
            return 0L;
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return Long.parseLong(value.toString());
    }

    private String buildKey(String productId) {

        return TenantContextHolder.getTenantId() + ":" + CACHE_NAME + ":" + productId;
    }
}
