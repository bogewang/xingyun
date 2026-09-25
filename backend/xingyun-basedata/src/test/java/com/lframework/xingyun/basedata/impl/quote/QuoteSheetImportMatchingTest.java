package com.lframework.xingyun.basedata.impl.quote;

import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.ProductUnit;
import com.lframework.xingyun.basedata.excel.quote.QuoteSheetImportModel;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.basedata.service.product.ProductUnitService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 报价导入必须严格匹配名称、规格和单位。 */
class QuoteSheetImportMatchingTest {
    /** 单个候选商品也不能覆盖导入行中不同的规格。 */
    @Test
    void shouldPreserveUnmatchedSpecWithSingleCandidate() {
        QuoteSheetServiceImpl service = service(product("p1", "500ml"));
        List<QuoteSheetImportModel> rows = Arrays.asList(row("500ml", "瓶"), row("1L", "瓶"));
        service.checkImport(rows);
        assertEquals("p1", rows.get(0).getProductId());
        assertNull(rows.get(1).getProductId());
        assertEquals("1L", rows.get(1).getSpec());
    }

    /** 同名不同规格应分别匹配商品，单位不同的行保留未匹配状态。 */
    @Test
    void shouldMatchEachSpecAndUnitExactly() {
        QuoteSheetServiceImpl service = service(product("p1", "500ml"), product("p2", "1L"));
        List<QuoteSheetImportModel> rows = Arrays.asList(row(" 500ml ", "瓶"), row("1L", "瓶"), row("1L", "箱"));
        service.checkImport(rows);
        assertEquals(3, rows.size());
        assertEquals("p1", rows.get(0).getProductId());
        assertEquals("p2", rows.get(1).getProductId());
        assertNull(rows.get(2).getProductId());
        assertEquals("箱", rows.get(2).getUnit());
    }

    /** 空规格仅匹配空规格商品，重复组合不能任意选择商品。 */
    @Test
    void shouldRequireExactBlankSpecAndUniqueMatch() {
        QuoteSheetImportModel missingSpec = row(null, "瓶");
        service(product("p1", "500ml")).checkImport(Collections.singletonList(missingSpec));
        assertNull(missingSpec.getProductId());
        service(product("p2", "")).checkImport(Collections.singletonList(missingSpec));
        assertEquals("p2", missingSpec.getProductId());
        QuoteSheetImportModel ambiguous = row("500ml", "瓶");
        service(product("p1", "500ml"), product("p2", "500ml")).checkImport(Collections.singletonList(ambiguous));
        assertNull(ambiguous.getProductId());
    }

    /** 创建使用真实导入逻辑和模拟商品数据的服务。 */
    private QuoteSheetServiceImpl service(Product... products) {
        QuoteSheetServiceImpl service = new QuoteSheetServiceImpl();
        ProductService productService = mock(ProductService.class);
        ProductUnitService unitService = mock(ProductUnitService.class);
        when(productService.selectByProductName(anyList())).thenReturn(Arrays.asList(products));
        for (Product product : products) {
            ProductUnit unit = new ProductUnit();
            unit.setUnitName("瓶");
            when(unitService.getAvailableByProductId(product.getId())).thenReturn(Collections.singletonList(unit));
            when(unitService.getAvailableByUnitName(product.getId(), "瓶")).thenReturn(unit);
        }
        ReflectionTestUtils.setField(service, "productService", productService);
        ReflectionTestUtils.setField(service, "productUnitService", unitService);
        return service;
    }

    /** 创建同名不同规格的商品主数据。 */
    private Product product(String id, String spec) {
        Product product = new Product();
        product.setId(id);
        product.setName("牛奶");
        product.setSpec(spec);
        return product;
    }

    /** 创建保留原始规格和单位的导入行。 */
    private QuoteSheetImportModel row(String spec, String unit) {
        QuoteSheetImportModel row = new QuoteSheetImportModel();
        row.setName("牛奶");
        row.setSpec(spec);
        row.setUnit(unit);
        return row;
    }
}
