package com.lframework.xingyun.basedata.impl.product;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.bo.product.info.GetProductBo;
import com.lframework.xingyun.basedata.bo.product.info.ProductSelectorBo;
import com.lframework.xingyun.basedata.entity.ProductCategory;
import com.lframework.xingyun.basedata.mappers.ProductMapper;
import com.lframework.xingyun.basedata.service.product.ProductCategoryService;
import com.lframework.xingyun.basedata.service.product.ProductPropertyRelationService;
import com.lframework.xingyun.basedata.service.product.ProductUnitService;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.basedata.vo.product.info.UpdateProductAvailableVo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

class ProductAvailabilityTest {

    @Test
    void shouldAllowEnabledProducts() throws Exception {
        ProductMapper mapper = mock(ProductMapper.class);
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(product("product-1", Boolean.TRUE)));

        ProductServiceImpl service = new ProductServiceImpl();
        setBaseMapper(service, mapper);

        assertDoesNotThrow(() -> service.assertAvailable(Collections.singletonList("product-1")));
    }

    @Test
    void shouldRejectDisabledProduct() throws Exception {
        ProductMapper mapper = mock(ProductMapper.class);
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(product("product-1", Boolean.FALSE)));

        ProductServiceImpl service = new ProductServiceImpl();
        setBaseMapper(service, mapper);

        DefaultClientException exception = assertThrows(DefaultClientException.class,
                () -> service.assertAvailable(Arrays.asList("product-1", "product-1")));

        assertEquals("商品已停用，无法新增业务单据！", exception.getMessage());
    }

    @Test
    void shouldIgnoreBlankAndDuplicateProductIdsWhenValidatingAvailability() throws Exception {
        ProductMapper mapper = mock(ProductMapper.class);
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(product("product-1", Boolean.TRUE)));

        ProductServiceImpl service = new ProductServiceImpl();
        setBaseMapper(service, mapper);

        service.assertAvailable(Arrays.asList("product-1", " ", null, "product-1"));

        ArgumentCaptor<Wrapper<Product>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper, times(1)).selectList(wrapperCaptor.capture());
        assertEquals(Collections.singletonList("product-1"), extractWrapperValues(wrapperCaptor.getValue()));
    }

    @Test
    void shouldUpdateAvailabilityInOneMapperCall() throws Exception {
        ProductMapper mapper = mock(ProductMapper.class);
        when(mapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        RecordingProductService recordingProductService = new RecordingProductService();
        ProductServiceImpl service = new ProductServiceImpl();
        setBaseMapper(service, mapper);
        setProductService(service, recordingProductService.proxy());

        UpdateProductAvailableVo vo = new UpdateProductAvailableVo();
        vo.setIds(Arrays.asList("product-1", "product-2", " ", "product-1"));
        vo.setAvailable(Boolean.FALSE);

        service.updateAvailable(vo);

        verify(mapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
        assertEquals(Arrays.asList("product-1", "product-2"), recordingProductService.getCleanCacheKeys());
    }

    /**
     * 验证历史数据按 ID 查询时仍可返回已停用商品。
     */
    @Test
    void shouldReadDisabledProductForHistoricalData() throws Exception {
        ProductMapper mapper = mock(ProductMapper.class);
        when(mapper.selectById("product-1")).thenReturn(product("product-1", Boolean.FALSE));

        ProductServiceImpl service = new ProductServiceImpl();
        setBaseMapper(service, mapper);

        Product result = service.findById("product-1");

        assertEquals(Boolean.FALSE, result.getAvailable());
        verify(mapper).selectById("product-1");
    }

    /**
     * 验证历史数据回显对象保留商品停用状态。
     */
    @Test
    void shouldExposeDisabledStatusInProductHistoryBos() {
        Product disabledProduct = product("product-1", Boolean.FALSE);
        ProductCategory category = new ProductCategory();
        category.setId("category-1");
        category.setName("分类");
        ProductCategoryService categoryService = mock(ProductCategoryService.class);
        when(categoryService.findById(any())).thenReturn(category);
        ProductUnitService unitService = mock(ProductUnitService.class);
        when(unitService.getByProductId("product-1")).thenReturn(Collections.emptyList());
        ProductPropertyRelationService relationService = mock(ProductPropertyRelationService.class);
        when(relationService.getByProductId("product-1")).thenReturn(Collections.emptyList());

        try (org.mockito.MockedStatic<com.lframework.starter.web.core.utils.ApplicationUtil> applicationUtil =
                     mockStatic(com.lframework.starter.web.core.utils.ApplicationUtil.class)) {
            applicationUtil.when(() -> com.lframework.starter.web.core.utils.ApplicationUtil.getBean(ProductCategoryService.class))
                    .thenReturn(categoryService);
            applicationUtil.when(() -> com.lframework.starter.web.core.utils.ApplicationUtil.getBean(ProductUnitService.class))
                    .thenReturn(unitService);
            applicationUtil.when(() -> com.lframework.starter.web.core.utils.ApplicationUtil.getBean(ProductPropertyRelationService.class))
                    .thenReturn(relationService);

            assertEquals(Boolean.FALSE, new ProductSelectorBo(disabledProduct).getAvailable());
            assertEquals(Boolean.FALSE, new GetProductBo(disabledProduct).getAvailable());
        }
    }

    @Test
    void shouldCleanCacheAfterTransactionCommit() throws Exception {
        ProductMapper mapper = mock(ProductMapper.class);
        when(mapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        RecordingProductService recordingProductService = new RecordingProductService();
        ProductServiceImpl service = new ProductServiceImpl();
        setBaseMapper(service, mapper);
        setProductService(service, recordingProductService.proxy());

        UpdateProductAvailableVo vo = new UpdateProductAvailableVo();
        vo.setIds(Arrays.asList("product-1", "product-2", "product-1"));
        vo.setAvailable(Boolean.TRUE);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.updateAvailable(vo);

            assertEquals(Collections.emptyList(), recordingProductService.getCleanCacheKeys());
            assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            assertEquals(Arrays.asList("product-1", "product-2"), recordingProductService.getCleanCacheKeys());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /**
     * 通过反射注入 BaseMapper，隔离 Spring 容器。
     *
     * @param service 商品服务实现
     * @param mapper 商品 Mapper
     * @throws Exception 反射注入失败时抛出
     */
    private void setBaseMapper(ProductServiceImpl service, ProductMapper mapper) throws Exception {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), Product.class);
        Class<?> type = service.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("baseMapper");
                field.setAccessible(true);
                field.set(service, mapper);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new IllegalStateException("未找到 BaseMapper 字段");
    }

    /**
     * 通过反射注入商品服务代理，用于校验缓存清理调用。
     *
     * @param service 商品服务实现
     * @param productService 商品服务代理
     * @throws Exception 反射注入失败时抛出
     */
    private void setProductService(ProductServiceImpl service, ProductService productService) throws Exception {
        Field field = ProductServiceImpl.class.getDeclaredField("productService");
        field.setAccessible(true);
        field.set(service, productService);
    }

    /**
     * 提取 Wrapper 中保存的参数值，便于断言去重后的商品 ID。
     *
     * @param wrapper MyBatis-Plus 条件包装器
     * @return 参数值列表
     */
    private List<Object> extractWrapperValues(Wrapper<Product> wrapper) {
        wrapper.getSqlSegment();
        return new ArrayList<>(((AbstractWrapper<Product, ?, ?>) wrapper).getParamNameValuePairs().values());
    }

    /**
     * 构造测试用商品。
     *
     * @param id 商品 ID
     * @param available 启用状态
     * @return 商品实体
     */
    private static Product product(String id, Boolean available) {
        Product product = new Product();
        product.setId(id);
        product.setAvailable(available);
        return product;
    }

    /**
     * 记录缓存清理调用的商品服务代理。
     */
    private static class RecordingProductService {

        private final List<String> cleanCacheKeys = new ArrayList<>();

        /**
         * 创建记录缓存清理调用的商品服务代理。
         *
         * @return 商品服务代理
         */
        private ProductService proxy() {
            return (ProductService) Proxy.newProxyInstance(ProductService.class.getClassLoader(),
                    new Class<?>[]{ProductService.class}, (proxy, method, args) -> {
                        if ("cleanCacheByKey".equals(method.getName())) {
                            cleanCacheKeys.add((String) args[0]);
                            return null;
                        }
                        if ("toString".equals(method.getName())) {
                            return "RecordingProductService";
                        }
                        throw new UnsupportedOperationException("未预期的商品服务调用：" + method.getName()
                                + Arrays.stream(args == null ? new Object[0] : args).map(String::valueOf)
                                .collect(Collectors.joining(",", "[", "]")));
                    });
        }

        /**
         * 获取缓存清理的商品 ID 列表。
         *
         * @return 缓存清理的商品 ID 列表
         */
        private List<String> getCleanCacheKeys() {
            return cleanCacheKeys;
        }
    }
}
