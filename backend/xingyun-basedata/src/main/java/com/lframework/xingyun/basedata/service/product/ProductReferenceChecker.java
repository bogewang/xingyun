package com.lframework.xingyun.basedata.service.product;

/**
 * 商品业务引用检查扩展点。
 */
public interface ProductReferenceChecker {

    /**
     * 判断商品是否被当前业务模块的数据引用。
     *
     * @param productId 商品 ID
     * @return 已被引用时返回 true
     */
    boolean hasReference(String productId);
}
