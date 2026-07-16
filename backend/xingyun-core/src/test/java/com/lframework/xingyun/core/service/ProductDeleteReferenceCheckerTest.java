package com.lframework.xingyun.core.service;

import org.testng.Assert;
import org.testng.annotations.Test;

class ProductDeleteReferenceCheckerTest {

    /**
     * 验证商品删除引用检查契约暴露正确的方法签名。
     *
     * @throws NoSuchMethodException 未找到目标方法时抛出
     */
    @Test
    void shouldExposeReferenceCheckMethod() throws NoSuchMethodException {
        Assert.assertEquals(ProductDeleteReferenceChecker.class
            .getMethod("isReferenced", String.class).getReturnType(), Boolean.TYPE);
    }
}
