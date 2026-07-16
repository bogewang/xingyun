package com.lframework.xingyun.sc.impl.product;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.sc.impl.purchase.PurchaseOrderServiceImpl;
import com.lframework.xingyun.sc.vo.purchase.CreatePurchaseOrderVo;
import com.lframework.xingyun.sc.vo.purchase.PurchaseProductVo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 验证新增业务单据在落库前接入商品启用状态校验。
 */
class ProductAvailabilityIntegrationTest {

    /**
     * 验证采购订单新增遇到停用商品时会在写入前失败。
     *
     * @throws Exception 反射注入失败时抛出
     */
    @Test
    void shouldRejectDisabledProductBeforeCreatingPurchaseOrder() throws Exception {
        ProductService productService = mock(ProductService.class);
        doThrow(new DefaultClientException("商品已停用，无法新增业务单据！"))
                .when(productService).assertAvailable(any());

        PurchaseOrderServiceImpl service = new PurchaseOrderServiceImpl();
        Field field = PurchaseOrderServiceImpl.class.getDeclaredField("productService");
        field.setAccessible(true);
        field.set(service, productService);

        PurchaseProductVo product = new PurchaseProductVo();
        product.setProductId("disabled-product");
        CreatePurchaseOrderVo vo = new CreatePurchaseOrderVo();
        vo.setProducts(Collections.singletonList(product));

        assertThrows(DefaultClientException.class, () -> service.create(vo));
        verify(productService).assertAvailable(Collections.singletonList("disabled-product"));
    }
}
