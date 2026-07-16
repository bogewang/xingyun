package com.lframework.xingyun.core.service;

/**
 * 商品删除前的跨模块引用检查。
 */
public interface ProductDeleteReferenceChecker {

    /**
     * 判断商品是否仍被业务单据引用。
     *
     * @param productId 商品 ID
     * @return 已引用时返回 true
     */
    boolean isReferenced(String productId);
}
