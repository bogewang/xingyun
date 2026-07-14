package com.lframework.xingyun.basedata.impl.product;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.lframework.starter.common.constants.PatternPool;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.*;
import com.lframework.starter.web.core.annotations.oplog.OpLog;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.event.DataChangeEventBuilder;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.*;
import com.lframework.starter.web.inner.service.GenerateCodeService;
import com.lframework.starter.web.inner.service.RecursionMappingService;
import com.lframework.xingyun.basedata.entity.*;
import com.lframework.xingyun.basedata.enums.BaseDataOpLogType;
import com.lframework.xingyun.basedata.enums.ColumnType;
import com.lframework.xingyun.basedata.enums.ProductCategoryNodeType;
import com.lframework.xingyun.basedata.events.DeleteProductEvent;
import com.lframework.xingyun.basedata.excel.product.ProductImportModel;
import com.lframework.xingyun.basedata.mappers.ProductMapper;
import com.lframework.xingyun.basedata.service.product.*;
import com.lframework.xingyun.basedata.service.supplier.SupplierService;
import com.lframework.xingyun.basedata.service.UnitService;
import com.lframework.xingyun.basedata.vo.product.brand.QueryProductBrandVo;
import com.lframework.xingyun.basedata.vo.product.info.*;
import com.lframework.xingyun.basedata.vo.product.property.realtion.CreateProductPropertyRelationVo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl extends BaseMpServiceImpl<ProductMapper, Product> implements
        ProductService {

    private static final DateTimeFormatter PRODUCT_CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    private static final Integer PRODUCT_CODE_TYPE = 10;

    @Autowired
    private RecursionMappingService recursionMappingService;

    @Autowired
    private ProductPropertyService productPropertyService;

    @Autowired
    private ProductPropertyItemService productPropertyItemService;

    @Autowired
    private ProductPropertyRelationService productPropertyRelationService;

    @Autowired
    private ProductUnitService productUnitService;

    @Autowired
    private ProductCategoryService productCategoryService;

    @Autowired
    private ProductBrandService productBrandService;
    @Autowired
    private SupplierService supplierService;
    @Autowired
    private UnitService unitService;

    @Autowired
    private GenerateCodeService generateCodeService;

    @Autowired
    private List<ProductReferenceChecker> productReferenceCheckers;

    @Override
    public PageResult<Product> query(Integer pageIndex, Integer pageSize, QueryProductVo vo) {

        Assert.greaterThanZero(pageIndex);
        Assert.greaterThanZero(pageSize);

        PageHelperUtil.startPage(pageIndex, pageSize);
        List<Product> datas = this.query(vo);

        return PageResultUtil.convert(new PageInfo<>(datas));
    }

    @Override
    public List<Product> query(QueryProductVo vo) {

        return getBaseMapper().query(vo);
    }

    @Override
    public PageResult<Product> selector(Integer pageIndex, Integer pageSize,
            QueryProductSelectorVo vo) {

        Assert.greaterThanZero(pageIndex);
        Assert.greaterThanZero(pageSize);

        PageHelperUtil.startPage(pageIndex, pageSize);
        List<Product> datas = getBaseMapper().selector(vo);

        return PageResultUtil.convert(new PageInfo<>(datas));
    }

    @Override
    public Integer queryCount(QueryProductVo vo) {

        return getBaseMapper().queryCount(vo);
    }

    @Cacheable(value = Product.CACHE_NAME, key = "@cacheVariables.tenantId() + #id", unless = "#result == null")
    @Override
    public Product findById(String id) {

        return getBaseMapper().selectById(id);
    }

    @Override
    public List<String> getIdNotInProductProperty(String propertyId) {

        return getBaseMapper().getIdNotInProductProperty(propertyId);
    }

    @Override
    public List<String> getIdByCategoryId(String categoryId) {

        return getBaseMapper().getIdByCategoryId(categoryId);
    }

    @OpLog(type = BaseDataOpLogType.class, name = "删除商品，ID：{}", params = "#id")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(String id) {

        assertNoProductReference(id, productReferenceCheckers);

        Wrapper<Product> updateWrapper = Wrappers.lambdaUpdate(Product.class)
                .set(Product::getAvailable, Boolean.FALSE).eq(Product::getId, id);
        getBaseMapper().update(updateWrapper);

        Product product = this.findById(id);

        DataChangeEventBuilder.publishLogicDelete(this, DeleteProductEvent.class, product);
    }

    /**
     * 校验商品未被业务数据引用。
     *
     * @param productId 商品 ID
     * @param productReferenceCheckers 商品引用检查器列表
     */
    static void assertNoProductReference(String productId, List<ProductReferenceChecker> productReferenceCheckers) {
        if (productReferenceCheckers.stream().anyMatch(checker -> checker.hasReference(productId))) {
            throw new DefaultClientException("商品已被业务单据或库存数据引用，无法删除！");
        }
    }

    @OpLog(type = BaseDataOpLogType.class, name = "新增商品，ID：{}, 编号：{}", params = { "#_result",
            "#vo.code" }, autoSaveParams = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String create(CreateProductVo vo) {
        if (StringUtils.isBlank(vo.getAlias())) {
            vo.setAlias(String.format("、%s、", vo.getName()));
        }

        String code = StringUtil.isBlank(vo.getCode()) ? generateCode() : vo.getCode();

        Wrapper<Product> checkWrapper = Wrappers.lambdaQuery(Product.class)
                .eq(Product::getCode, code).eq(Product::getAvailable, Boolean.TRUE);
        if (getBaseMapper().selectCount(checkWrapper) > 0) {
            throw new DefaultClientException("编号重复，请重新输入！");
        }

        if (StringUtil.isNotBlank(vo.getSkuCode())) {
            checkWrapper = Wrappers.lambdaQuery(Product.class).eq(Product::getSkuCode, vo.getSkuCode())
                    .eq(Product::getAvailable, Boolean.TRUE);
            if (getBaseMapper().selectCount(checkWrapper) > 0) {
                throw new DefaultClientException("商品SKU编号重复，请重新输入！");
            }
        }

        checkNameSpecUnit(vo);

        Product data = new Product();
        data.setId(IdUtil.getId());
        data.setCode(code);
        data.setName(vo.getName());
        if (StringUtil.isNotBlank(vo.getShortName())) {
            data.setShortName(vo.getShortName());
        }
        if (StringUtil.isNotBlank(vo.getSkuCode())) {
            data.setSkuCode(vo.getSkuCode());
        }
        if (StringUtil.isNotBlank(vo.getExternalCode())) {
            data.setExternalCode(vo.getExternalCode());
        }

        if (StringUtil.isNotBlank(vo.getBrandId())) {
            data.setBrandId(vo.getBrandId());
        }
        data.setCategoryId(vo.getCategoryId());

        ProductCategory productCategory = productCategoryService.findById(data.getCategoryId());
        Wrapper<ProductCategory> checkCategoryWrapper = Wrappers.lambdaQuery(
                ProductCategory.class).eq(ProductCategory::getParentId, productCategory.getId())
                .eq(ProductCategory::getAvailable, Boolean.TRUE);

        if (productCategoryService.count(checkCategoryWrapper) > 0) {
            throw new DefaultClientException("“商品分类”不是末级分类，请选择末级分类");
        }

        if (StringUtil.isNotBlank(vo.getSpec())) {
            data.setSpec(vo.getSpec());
        }

        if (StringUtil.isNotBlank(vo.getUnit())) {
            data.setUnit(vo.getUnit());
        }

        data.setTaxRate(vo.getTaxRate() == null ? BigDecimal.ZERO : vo.getTaxRate());
        data.setSaleTaxRate(vo.getSaleTaxRate() == null ? BigDecimal.ZERO : vo.getSaleTaxRate());
        data.setWeight(vo.getWeight());
        data.setVolume(vo.getVolume());
        data.setAlias(StringUtil.isBlank(vo.getAlias()) ? null : vo.getAlias());
        data.setDefaultSupplier(
                StringUtil.isBlank(vo.getDefaultSupplier()) ? null : vo.getDefaultSupplier());
        data.setRemark(StringUtil.isBlank(vo.getRemark()) ? null : vo.getRemark());
        data.setRemark2(StringUtil.isBlank(vo.getRemark2()) ? null : vo.getRemark2());

        data.setAvailable(Boolean.TRUE);

        handlePurchasePrice(vo, data);
        handleSalePrice(vo, data);
        handleRetailPrice(vo, data);

        getBaseMapper().insert(data);
        saveUnits(data, vo.getUnits());

        if (!CollectionUtil.isEmpty(vo.getProperties())) {
            // 商品和商品属性的关系
            for (ProductPropertyRelationVo property : vo.getProperties()) {
                ProductProperty productProperty = productPropertyService.findById(property.getId());
                if (productProperty == null) {
                    throw new DefaultClientException("商品属性不存在！");
                }
                if (productProperty.getColumnType() == ColumnType.SINGLE) {
                    ProductPropertyItem propertyItem = productPropertyItemService.findById(
                            property.getText());
                    if (propertyItem == null) {
                        throw new DefaultClientException("商品属性值不存在！");
                    }

                    CreateProductPropertyRelationVo createProductPropertyRelationVo = new CreateProductPropertyRelationVo();
                    createProductPropertyRelationVo.setProductId(data.getId());
                    createProductPropertyRelationVo.setPropertyId(productProperty.getId());
                    createProductPropertyRelationVo.setPropertyItemId(propertyItem.getId());

                    productPropertyRelationService.create(createProductPropertyRelationVo);
                } else if (productProperty.getColumnType() == ColumnType.MULTIPLE) {

                    List<String> propertyItemIds = JsonUtil.parseList(property.getText(), String.class);
                    for (String propertyItemId : propertyItemIds) {
                        CreateProductPropertyRelationVo createProductPropertyRelationVo = new CreateProductPropertyRelationVo();
                        createProductPropertyRelationVo.setProductId(data.getId());
                        createProductPropertyRelationVo.setPropertyId(productProperty.getId());
                        createProductPropertyRelationVo.setPropertyItemId(propertyItemId);

                        productPropertyRelationService.create(createProductPropertyRelationVo);
                    }

                } else if (productProperty.getColumnType() == ColumnType.CUSTOM) {

                    CreateProductPropertyRelationVo createProductPropertyRelationVo = new CreateProductPropertyRelationVo();
                    createProductPropertyRelationVo.setProductId(data.getId());
                    createProductPropertyRelationVo.setPropertyId(productProperty.getId());
                    createProductPropertyRelationVo.setPropertyText(property.getText());
                    productPropertyRelationService.create(createProductPropertyRelationVo);
                } else {
                    throw new DefaultClientException("商品属性字段类型不存在！");
                }
            }
        }

        return data.getId();
    }

    @Override
    public String generateCode() {
        while (true) {
            String code = generateCodeService.generate(PRODUCT_CODE_TYPE);
            if (!existsProductCode(code)) {
                return code;
            }
        }
    }

    private void handleRetailPrice(CreateProductVo vo, Product data) {
        // if (vo.getRetailPrice() == null) {
        //     throw new DefaultClientException("零售价不能为空！");
        // }
        //
        // if (NumberUtil.lt(vo.getRetailPrice(), 0D)) {
        //     throw new DefaultClientException("零售价不允许小于0！");
        // }

        data.setRetailPrice(vo.getRetailPrice());
    }

    private void handleSalePrice(CreateProductVo vo, Product data) {
        // if (vo.getSalePrice() == null) {
        //     throw new DefaultClientException("销售价不能为空！");
        // }
        //
        // if (NumberUtil.lt(vo.getSalePrice(), 0)) {
        //     throw new DefaultClientException("销售价不允许小于0！");
        // }

        data.setSalePrice(vo.getSalePrice());
    }

    private void handlePurchasePrice(CreateProductVo vo, Product data) {
        // if (vo.getPurchasePrice() == null) {
        //     throw new DefaultClientException("采购价不能为空！");
        // }
        //
        // if (NumberUtil.lt(vo.getPurchasePrice(), 0)) {
        //     throw new DefaultClientException("采购价不允许小于0！");
        // }
        data.setPurchasePrice(vo.getPurchasePrice());
    }

    @OpLog(type = BaseDataOpLogType.class, name = "修改商品，ID：{}, 编号：{}", params = { "#id",
            "#code" })
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(UpdateProductVo vo) {

        Product data = getBaseMapper().selectById(vo.getId());
        if (ObjectUtil.isNull(data)) {
            throw new DefaultClientException("商品不存在！");
        }

        Wrapper<Product> checkWrapper = Wrappers.lambdaQuery(Product.class)
                .eq(Product::getCode, vo.getCode()).eq(Product::getAvailable, Boolean.TRUE)
                .ne(Product::getId, vo.getId());
        if (getBaseMapper().selectCount(checkWrapper) > 0) {
            throw new DefaultClientException("编号重复，请重新输入！");
        }

        if (StringUtil.isNotBlank(vo.getSkuCode())) {
            checkWrapper = Wrappers.lambdaQuery(Product.class).eq(Product::getAvailable, Boolean.TRUE)
                    .eq(Product::getSkuCode, vo.getSkuCode())
                    .ne(Product::getId, vo.getId());
            if (getBaseMapper().selectCount(checkWrapper) > 0) {
                throw new DefaultClientException("商品SKU编号重复，请重新输入！");
            }
        }

        ProductCategory productCategory = productCategoryService.findById(vo.getCategoryId());
        Wrapper<ProductCategory> checkCategoryWrapper = Wrappers.lambdaQuery(
                ProductCategory.class).eq(ProductCategory::getParentId, productCategory.getId())
                .eq(ProductCategory::getAvailable, Boolean.TRUE);
        if (productCategoryService.count(checkCategoryWrapper) > 0) {
            throw new DefaultClientException(
                    "“商品分类”不是末级分类，请选择末级分类");
        }

        LambdaUpdateWrapper<Product> updateWrapper = Wrappers.lambdaUpdate(Product.class)
                .set(Product::getCode, vo.getCode()).set(Product::getName, vo.getName())
                .set(Product::getSkuCode, vo.getSkuCode())
                .set(Product::getExternalCode,
                        StringUtil.isBlank(vo.getExternalCode()) ? null : vo.getExternalCode())
                .set(Product::getSpec, StringUtil.isBlank(vo.getSpec()) ? null : vo.getSpec())
                .set(Product::getUnit, StringUtil.isBlank(vo.getUnit()) ? null : vo.getUnit())
                .set(Product::getShortName,
                        StringUtil.isBlank(vo.getShortName()) ? null : vo.getShortName())
                .set(Product::getCategoryId,
                        StringUtil.isBlank(vo.getCategoryId()) ? null : vo.getCategoryId())
                .set(Product::getBrandId, StringUtil.isBlank(vo.getBrandId()) ? null : vo.getBrandId())
                .set(Product::getTaxRate, vo.getTaxRate() == null ? BigDecimal.ZERO : vo.getTaxRate())
                .set(Product::getSaleTaxRate,
                        vo.getSaleTaxRate() == null ? BigDecimal.ZERO : vo.getSaleTaxRate())
                .set(Product::getWeight, vo.getWeight())
                .set(Product::getVolume, vo.getVolume())
                .set(Product::getSalePrice, vo.getSalePrice())
                .set(Product::getPurchasePrice, vo.getPurchasePrice())
                .set(Product::getRetailPrice, vo.getRetailPrice())
                .set(Product::getAlias, StringUtil.isBlank(vo.getAlias()) ? null : vo.getAlias())
                .set(Product::getDefaultSupplier,
                        StringUtil.isBlank(vo.getDefaultSupplier()) ? null : vo.getDefaultSupplier())
                .set(Product::getRemark, StringUtil.isBlank(vo.getRemark()) ? null : vo.getRemark())
                .set(Product::getRemark2, StringUtil.isBlank(vo.getRemark2()) ? null : vo.getRemark2())
                .eq(Product::getId, vo.getId());

        getBaseMapper().update(updateWrapper);
        if (vo.getUnits() != null) {
            data.setUnit(StringUtil.isBlank(vo.getUnit()) ? null : vo.getUnit());
            saveUnits(data, vo.getUnits());
        }

        productPropertyRelationService.deleteByProductId(data.getId());
        if (!CollectionUtil.isEmpty(vo.getProperties())) {
            // 商品和商品属性的关系
            for (ProductPropertyRelationVo property : vo.getProperties()) {
                ProductProperty productProperty = productPropertyService.findById(property.getId());
                if (productProperty == null) {
                    throw new DefaultClientException("商品属性不存在！");
                }
                if (productProperty.getColumnType() == ColumnType.SINGLE) {
                    ProductPropertyItem propertyItem = productPropertyItemService.findById(
                            property.getText());
                    if (propertyItem == null) {
                        throw new DefaultClientException("商品属性值不存在！");
                    }

                    CreateProductPropertyRelationVo createProductPropertyRelationVo = new CreateProductPropertyRelationVo();
                    createProductPropertyRelationVo.setProductId(data.getId());
                    createProductPropertyRelationVo.setPropertyId(productProperty.getId());
                    createProductPropertyRelationVo.setPropertyItemId(propertyItem.getId());

                    productPropertyRelationService.create(createProductPropertyRelationVo);
                } else if (productProperty.getColumnType() == ColumnType.MULTIPLE) {

                    List<String> propertyItemIds = JsonUtil.parseList(property.getText(), String.class);
                    for (String propertyItemId : propertyItemIds) {
                        CreateProductPropertyRelationVo createProductPropertyRelationVo = new CreateProductPropertyRelationVo();
                        createProductPropertyRelationVo.setProductId(data.getId());
                        createProductPropertyRelationVo.setPropertyId(productProperty.getId());
                        createProductPropertyRelationVo.setPropertyItemId(propertyItemId);

                        productPropertyRelationService.create(createProductPropertyRelationVo);
                    }

                } else if (productProperty.getColumnType() == ColumnType.CUSTOM) {

                    CreateProductPropertyRelationVo createProductPropertyRelationVo = new CreateProductPropertyRelationVo();
                    createProductPropertyRelationVo.setProductId(data.getId());
                    createProductPropertyRelationVo.setPropertyId(productProperty.getId());
                    createProductPropertyRelationVo.setPropertyText(property.getText());
                    productPropertyRelationService.create(createProductPropertyRelationVo);
                } else {
                    throw new DefaultClientException("商品属性字段类型不存在！");
                }
            }
        }

        OpLogUtil.setVariable("id", data.getId());
        OpLogUtil.setVariable("code", vo.getCode());
        OpLogUtil.setExtra(vo);
    }

    @Override
    public List<Product> getByCategoryIds(List<String> categoryIds) {

        if (CollectionUtil.isEmpty(categoryIds)) {
            return CollectionUtil.emptyList();
        }

        // 根据categoryIds查询所有叶子节点
        List<String> children = new ArrayList<>();
        for (String categoryId : categoryIds) {
            children.addAll(recursionMappingService.getNodeChildIds(categoryId,
                    ProductCategoryNodeType.class));
        }

        children.addAll(categoryIds);

        children = children.stream().distinct().collect(Collectors.toList());

        List<Product> datas = getBaseMapper().getByCategoryIds(children);

        return datas;
    }

    @Override
    public List<Product> getByBrandIds(List<String> brandIds) {

        if (CollectionUtil.isEmpty(brandIds)) {
            return CollectionUtil.emptyList();
        }

        return getBaseMapper().getByBrandIds(brandIds);
    }

    @CacheEvict(value = Product.CACHE_NAME, key = "@cacheVariables.tenantId() + #key")
    @Override
    public void cleanCacheByKey(Serializable key) {

    }

    @Override
    public List<Product> selectAllAvailable() {
        Wrapper<Product> checkWrapper = Wrappers.lambdaQuery(Product.class)
                .eq(Product::getAvailable, Boolean.TRUE);
        return getBaseMapper().selectList(checkWrapper);
    }

    @Override
    public List<Product> selectByProductName(List<String> productNames) {
        if (CollectionUtils.isEmpty(productNames)) {
            return CollectionUtil.emptyList();
        }
        Wrapper<Product> checkWrapper = Wrappers.lambdaQuery(Product.class)
                .in(Product::getName, productNames)
                .eq(Product::getAvailable, Boolean.TRUE);
        return getBaseMapper().selectList(checkWrapper);
    }

    @Override
    public List<Product> selectByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return CollectionUtil.emptyList();
        }
        Wrapper<Product> checkWrapper = Wrappers.lambdaQuery(Product.class)
                .in(Product::getId, ids)
                .eq(Product::getAvailable, Boolean.TRUE);
        return getBaseMapper().selectList(checkWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updatePrice(String id, BigDecimal salePrice, BigDecimal purchasePrice) {
        Assert.notBlank(id);

        LambdaUpdateWrapper<Product> updateWrapper = Wrappers.lambdaUpdate(Product.class)
                .eq(Product::getId, id);

        if (salePrice != null) {
            updateWrapper.set(Product::getSalePrice, salePrice);
        }
        if (purchasePrice != null) {
            updateWrapper.set(Product::getPurchasePrice, purchasePrice);
        }

        if (updateWrapper.getSqlSet() == null) {
            return;
        }

        getBaseMapper().update(null, updateWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void importExcel(List<ProductImportModel> list) {
        if (CollectionUtil.isEmpty(list)) {
            throw new DefaultClientException("导入数据为空！");
        }

        this.check(list);
        ProductImportPersistBatch persistBatch = this.buildProducts(list);

        if (CollectionUtils.isNotEmpty(persistBatch.getInserts())) {
            super.saveBatch(persistBatch.getInserts());
        }
        if (CollectionUtils.isNotEmpty(persistBatch.getUpdates())) {
            super.updateBatchById(persistBatch.getUpdates());
        }
        saveDefaultUnits(persistBatch.getInserts(), persistBatch.getUpdates());
    }

    private void saveDefaultUnits(List<Product> inserts, List<Product> updates) {
        List<Product> products = new ArrayList<>(inserts.size() + updates.size());
        products.addAll(inserts);
        products.addAll(updates);
        if (CollectionUtil.isEmpty(products)) {
            return;
        }

        Set<String> productIds = products.stream().map(Product::getId).collect(Collectors.toSet());
        Set<String> configuredProductIds = productUnitService.list(Wrappers.lambdaQuery(ProductUnit.class)
                .in(ProductUnit::getProductId, productIds)).stream().map(ProductUnit::getProductId)
                .collect(Collectors.toSet());
        Set<String> unitIds = products.stream().filter(product -> !configuredProductIds.contains(product.getId()))
                .map(Product::getUnit).filter(StringUtil::isNotBlank).collect(Collectors.toSet());
        Map<String, String> unitNames = CollectionUtil.isEmpty(unitIds) ? Collections.emptyMap()
                : unitService.list(Wrappers.lambdaQuery(Unit.class).in(Unit::getId, unitIds)
                        .eq(Unit::getAvailable, Boolean.TRUE)).stream()
                        .collect(Collectors.toMap(Unit::getId, Unit::getName));
        List<ProductUnit> defaultUnits = buildDefaultProductUnits(products, configuredProductIds, unitNames);
        if (CollectionUtils.isNotEmpty(defaultUnits)) {
            defaultUnits.forEach(unit -> unit.setId(IdUtil.getId()));
            productUnitService.saveBatch(defaultUnits);
        }
    }

    static List<ProductUnit> buildDefaultProductUnits(List<Product> products, Set<String> configuredProductIds,
            Map<String, String> unitNames) {
        List<ProductUnit> units = new ArrayList<>();
        for (Product product : products) {
            if (configuredProductIds.contains(product.getId())) {
                continue;
            }
            String unitName = unitNames.get(product.getUnit());
            if (StringUtil.isBlank(product.getUnit()) || StringUtil.isBlank(unitName)) {
                throw new DefaultClientException("主单位不存在或已停用！");
            }
            ProductUnit unit = new ProductUnit();
            unit.setProductId(product.getId());
            unit.setUnitName(unitName);
            unit.setConversionRate(BigDecimal.ONE);
            unit.setBaseUnit(Boolean.TRUE);
            unit.setAvailable(Boolean.TRUE);
            unit.setSortNo(0);
            units.add(unit);
        }
        return units;
    }

    private ProductImportPersistBatch buildProducts(List<ProductImportModel> list) {
        if (CollectionUtil.isEmpty(list)) {
            return new ProductImportPersistBatch(CollectionUtil.emptyList(), CollectionUtil.emptyList());
        }
        List<String> supplierNames = list.stream().map(ProductImportModel::getDefaultSupplier).collect(Collectors.toList());
        Map<String, String> map = supplierService.queryByNames(supplierNames).stream().collect(Collectors.toMap(Supplier::getName, Supplier::getId, (a, b) -> b));

        List<Product> inserts = Lists.newArrayList();
        List<Product> updates = Lists.newArrayList();
        list.forEach(data -> {
            Product record = BeanUtil.copyProperties(data, Product.class);
            if (StringUtil.isNotBlank(data.getUnit())) {
                Unit unit = unitService.getOne(Wrappers.lambdaQuery(Unit.class)
                        .eq(Unit::getName, data.getUnit()).eq(Unit::getAvailable, Boolean.TRUE));
                if (unit == null) {
                    throw new DefaultClientException(String.format("单位%s不存在或已停用", data.getUnit()));
                }
                record.setUnit(unit.getId());
            }
            boolean isNew = StringUtil.isBlank(record.getId());
            if (isNew) {
                record.setId(IdUtil.getId());
            }
            record.setTaxRate(data.getTaxRate() == null ? BigDecimal.ZERO : data.getTaxRate());
            record.setSaleTaxRate(data.getSaleTaxRate() == null ? BigDecimal.ZERO : data.getSaleTaxRate());
            record.setAvailable(Boolean.TRUE);
            record.setSalePrice(data.getSalePrice() == null ? BigDecimal.ZERO : data.getSalePrice());
            record.setPurchasePrice(data.getPurchasePrice() == null ? BigDecimal.ZERO : data.getPurchasePrice());
            record.setRetailPrice(data.getRetailPrice() == null ? BigDecimal.ZERO : data.getRetailPrice());
            if (StringUtil.isNotBlank(data.getDefaultSupplier())) {
                if (!map.containsKey(data.getDefaultSupplier())) {
                    throw new DefaultClientException(String.format("供应商%s不存在", data.getDefaultSupplier()));
                }
                record.setDefaultSupplier(map.get(data.getDefaultSupplier()));
            }
            if (isNew) {
                inserts.add(record);
            } else {
                updates.add(record);
            }

            data.setId(record.getId());

        });

        return new ProductImportPersistBatch(inserts, updates);

    }

    private static class ProductImportPersistBatch {

        private final List<Product> inserts;

        private final List<Product> updates;

        private ProductImportPersistBatch(List<Product> inserts, List<Product> updates) {
            this.inserts = inserts;
            this.updates = updates;
        }

        private List<Product> getInserts() {
            return inserts;
        }

        private List<Product> getUpdates() {
            return updates;
        }
    }

    private void check(List<ProductImportModel> list) {
        Set<String> checkCodeSet = new HashSet<>();
        Map<String, Integer> checkCodeRowMap = new HashMap<>();
        Set<String> checkSkuCodeSet = new HashSet<>();
        Map<String, Integer> checkSkuCodeRowMap = new HashMap<>();
        Map<String, Integer> checkNameSpecUnitMap = new HashMap<>();
        Set<String> importCodes = list.stream().map(ProductImportModel::getCode)
                .filter(StringUtil::isNotBlank).collect(Collectors.toSet());
        Set<String> importSkuCodes = list.stream().map(ProductImportModel::getSkuCode)
                .filter(StringUtil::isNotBlank).collect(Collectors.toSet());
        Set<String> importNames = list.stream().map(ProductImportModel::getName)
                .filter(StringUtil::isNotBlank).collect(Collectors.toSet());

        Map<String, Product> availableCodes = queryAvailableProductsByCodes(importCodes).stream()
                .collect(Collectors.toMap(Product::getCode, item -> item, (a, b) -> a));
        Map<String, Product> availableSkuCodes = queryAvailableProductsBySkuCodes(importSkuCodes).stream()
                .collect(Collectors.toMap(Product::getSkuCode, item -> item, (a, b) -> a));
        Map<String, Product> availableNameSpecUnitKeys = queryAvailableProductsByNames(importNames).stream()
                .collect(Collectors.toMap(this::buildNameSpecUnitKey, item -> item));
        // 检查分类编号是否重复
        List<ProductCategory> availableCategories = productCategoryService.getAllProductCategories();
        Map<String, String> categoryMap = availableCategories.stream()
                .collect(Collectors.toMap(ProductCategory::getName, ProductCategory::getId));
        // 检查品牌编号是否重复
        List<ProductBrand> brands = productBrandService.query(new QueryProductBrandVo());
        Map<String, String> brandCodeMap = brands.stream()
                .collect(Collectors.toMap(ProductBrand::getCode, ProductBrand::getId, (a, b) -> a));
        Map<String, String> brandNameMap = brands.stream()
                .collect(Collectors.toMap(ProductBrand::getName, ProductBrand::getId, (a, b) -> a));
        // 检查分类是否是最下层
        Set<String> parentCategoryIds = new HashSet<>();
        Set<String> generatedCodePool = new HashSet<>(queryCodesByPrefix(buildTodayProductCodePrefix()));
        generatedCodePool.addAll(importCodes);

        for (int i = 0; i < list.size(); i++) {
            ProductImportModel data = list.get(i);
            if (StringUtil.isBlank(data.getCode())) {
                data.setCode(generateProductCode(checkCodeSet, generatedCodePool));
            }

            checkRules(list, checkCodeSet, checkCodeRowMap, checkSkuCodeSet, checkSkuCodeRowMap, checkNameSpecUnitMap,
                    availableCodes, availableSkuCodes, availableNameSpecUnitKeys, categoryMap, brandCodeMap, brandNameMap,
                    parentCategoryIds, i);
        }

        checkIsLeafCategory(list, new ArrayList<>(parentCategoryIds));

    }

    /**
     * 检查分类是否是末级分类
     * 
     * @param list
     * @param parentCategoryIds
     */
    private void checkIsLeafCategory(List<ProductImportModel> list, List<String> parentCategoryIds) {
        Map<String, List<ProductCategory>> categoryMapByParentId = productCategoryService
                .getCategoryByParentIds(parentCategoryIds)
                .stream()
                .collect(Collectors.groupingBy(ProductCategory::getParentId));
        // 判断分类是否是末级分类
        for (int i = 0; i < list.size(); i++) {
            ProductImportModel data = list.get(i);
            if (categoryMapByParentId.containsKey(data.getCategoryId())) {
                throw new DefaultClientException("第" + (i + 2) + "行“商品分类”不是末级分类，请使用末级分类");
            }
        }
    }

    /**
     * 检查商品数据是否符合要求
     * 
     * @param list
     * @param checkCodeList
     * @param checkSkuCodeList
     * @param checkNameSpecUnitMap
     * @param availableCodes
     * @param availableSkuCodes
     * @param availableNameSpecUnitKeys
     * @param categoryMap
     * @param brandCodeMap
     * @param brandNameMap
     * @param parentCategoryIds
     * @param i
     */
    private void checkRules(List<ProductImportModel> list, Set<String> checkCodeSet, Map<String, Integer> checkCodeRowMap,
            Set<String> checkSkuCodeSet, Map<String, Integer> checkSkuCodeRowMap, Map<String, Integer> checkNameSpecUnitMap,
            Map<String, Product> availableCodes, Map<String, Product> availableSkuCodes, Map<String, Product> availableNameSpecUnitKeys,
            Map<String, String> categoryMap, Map<String, String> brandCodeMap, Map<String, String> brandNameMap,
            Set<String> parentCategoryIds, int i) {
        ProductImportModel data = list.get(i);
        int rowIndex = (i + 2);
        checkCode(checkCodeSet, checkCodeRowMap, availableCodes, data, rowIndex);

        if (StringUtil.isBlank(data.getName())) {
            throw new DefaultClientException("第" + rowIndex + "行“名称”不能为空");
        }

        if (StringUtil.isNotBlank(data.getSkuCode())) {
            checkSkuCode(checkSkuCodeSet, checkSkuCodeRowMap, availableSkuCodes, data, rowIndex);
        }

        checkNameSpecUnit(checkNameSpecUnitMap, availableNameSpecUnitKeys, data, rowIndex);
        checkCategory(categoryMap, parentCategoryIds, data, rowIndex);

        if (StringUtil.isNotBlank(data.getBrandName())) {
            String brandId = brandCodeMap.get(data.getBrandName());
            if (brandId == null) {
                brandId = brandNameMap.get(data.getBrandName());
            }
            if (brandId == null) {
                throw new DefaultClientException(String.format("第%d行品牌：%s 不存在，请检查", rowIndex, data.getBrandName()));
            }
            data.setBrandId(brandId);
        }

        if (data.getTaxRate() != null) {
            if (NumberUtil.lt(data.getTaxRate(), 0)) {
                throw new DefaultClientException("第" + rowIndex + "行“进项税率（%）”不允许小于0");
            }

            if (!NumberUtil.isNumberPrecision(data.getTaxRate(), 2)) {
                throw new DefaultClientException("第" + rowIndex + "行“进项税率（%）”最多允许2位小数");
            }
        }

        if (data.getSaleTaxRate() != null) {
            if (NumberUtil.lt(data.getSaleTaxRate(), 0)) {
                throw new DefaultClientException("第" + rowIndex + "行“销项税率（%）”不允许小于0");
            }

            if (!NumberUtil.isNumberPrecision(data.getSaleTaxRate(), 2)) {
                throw new DefaultClientException("第" + rowIndex + "行“销项税率（%）”最多允许2位小数");
            }
        }

        checkPrice(data, rowIndex);
    }

    /**
     * 检查分类是否符合要求
     * 
     * @param categoryMap
     * @param parentCategoryIds
     * @param data
     * @param rowIndex
     */
    private void checkCategory(Map<String, String> categoryMap, Set<String> parentCategoryIds, ProductImportModel data,
            int rowIndex) {
        if (StringUtil.isBlank(data.getCategoryName())) {
            throw new DefaultClientException("第" + rowIndex + "行“商品分类”不能为空");
        }

        if (!categoryMap.containsKey(data.getCategoryName())) {
            throw new DefaultClientException(String.format("第%s行商品分类:：“%s”不存在，请检查", rowIndex, data.getCategoryName()));
        }

        data.setCategoryId(categoryMap.get(data.getCategoryName()));
        parentCategoryIds.add(data.getCategoryId());
    }

    /**
     * 检查价格是否符合要求
     * 
     * @param data
     * @param rowIndex
     */
    private void checkPrice(ProductImportModel data, int rowIndex) {
        if (data.getPurchasePrice() != null) {
            if (!NumberUtil.isNumberPrecision(data.getPurchasePrice(), 6)) {
                throw new DefaultClientException("第" + rowIndex + "行“采购价（元）”最多允许6位小数");
            }
            if (NumberUtil.lt(data.getPurchasePrice(), 0)) {
                throw new DefaultClientException("第" + rowIndex + "行“采购价（元）”不允许小于0");
            }
        }

        if (data.getSalePrice() != null) {
            if (!NumberUtil.isNumberPrecision(data.getSalePrice(), 6)) {
                throw new DefaultClientException("第" + rowIndex + "行“销售价（元）”最多允许6位小数");
            }
            if (NumberUtil.lt(data.getSalePrice(), 0)) {
                throw new DefaultClientException("第" + rowIndex + "行“销售价（元）”不允许小于0");
            }
        }

        if (data.getRetailPrice() != null) {
            if (!NumberUtil.isNumberPrecision(data.getRetailPrice(), 6)) {
                throw new DefaultClientException("第" + rowIndex + "行“零售价（元）”最多允许6位小数");
            }
            if (NumberUtil.lt(data.getRetailPrice(), 0)) {
                throw new DefaultClientException("第" + rowIndex + "行“零售价（元）”不允许小于0");
            }
        }
    }

    private void checkSkuCode(Set<String> checkSkuCodeSet, Map<String, Integer> checkSkuCodeRowMap,
            Map<String, Product> availableSkuCodes, ProductImportModel data, int rowIndex) {
        if (checkSkuCodeSet.contains(data.getSkuCode())) {
            throw new DefaultClientException(
                    "第" + rowIndex + "行“SKU编号”与第" + checkSkuCodeRowMap.get(data.getSkuCode()) + "行重复");
        }
        checkSkuCodeSet.add(data.getSkuCode());
        checkSkuCodeRowMap.put(data.getSkuCode(), rowIndex);
        if (availableSkuCodes.containsKey(data.getSkuCode()) && data.getId() == null) {
            data.setId(availableSkuCodes.get(data.getSkuCode()).getId());
        }
    }

    private void checkCode(Set<String> checkCodeSet, Map<String, Integer> checkCodeRowMap,
            Map<String, Product> availableCodes, ProductImportModel data, int rowIndex) {
        if (StringUtil.isBlank(data.getCode())) {
            throw new DefaultClientException("第" + rowIndex + "行“编号”不能为空");
        }
        if (!RegUtil.isMatch(PatternPool.PATTERN_CODE, data.getCode())) {
            throw new DefaultClientException("第" + rowIndex + "行“编号”必须由字母、数字、“-_.”组成，长度不能超过20位");
        }
        if (checkCodeSet.contains(data.getCode())) {
            throw new DefaultClientException(
                    "第" + rowIndex + "行“编号”与第" + checkCodeRowMap.get(data.getCode()) + "行重复");
        }
        checkCodeSet.add(data.getCode());
        checkCodeRowMap.put(data.getCode(), rowIndex);
        if (availableCodes.containsKey(data.getCode())) {
            data.setId(availableCodes.get(data.getCode()).getId());
        }
    }

    /**
     * 名称+规格+单位 校验
     * @param checkNameSpecUnitMap
     * @param availableNameSpecUnitKeys
     * @param data
     * @param rowIndex
     */
    private void checkNameSpecUnit(Map<String, Integer> checkNameSpecUnitMap, Map<String, Product> availableNameSpecUnitKeys,
            ProductImportModel data, int rowIndex) {
        String key = buildNameSpecUnitKey(data);
        Integer existsRowIndex = checkNameSpecUnitMap.get(key);
        if (existsRowIndex != null) {
            throw new DefaultClientException(
                    "第" + rowIndex + "行“名称+规格+单位”与第" + existsRowIndex + "行重复");
        }
        if (availableNameSpecUnitKeys.containsKey(key) && data.getId() == null) {
            data.setId(availableNameSpecUnitKeys.get(key).getId());
        }
        checkNameSpecUnitMap.put(key, rowIndex);
    }

    private String generateProductCode(Set<String> checkCodeSet, Set<String> usedCodes) {
        String prefix = buildTodayProductCodePrefix();
        int sequence = 1;
        while (true) {
            String code = prefix + String.format("%05d", sequence);
            if (!checkCodeSet.contains(code) && usedCodes.add(code)) {
                return code;
            }
            sequence++;
        }
    }

    private String buildTodayProductCodePrefix() {
        return "P" + LocalDate.now().format(PRODUCT_CODE_DATE_FORMATTER);
    }

    private List<Product> queryAvailableProductsByCodes(Set<String> codes) {
        if (CollectionUtil.isEmpty(codes)) {
            return CollectionUtil.emptyList();
        }

        Wrapper<Product> checkWrapper = Wrappers.lambdaQuery(Product.class)
                .in(Product::getCode, codes)
                .eq(Product::getAvailable, Boolean.TRUE);
        return getBaseMapper().selectList(checkWrapper);
    }

    private List<Product> queryAvailableProductsBySkuCodes(Set<String> skuCodes) {
        if (CollectionUtil.isEmpty(skuCodes)) {
            return CollectionUtil.emptyList();
        }

        Wrapper<Product> checkWrapper = Wrappers.lambdaQuery(Product.class)
                .in(Product::getSkuCode, skuCodes)
                .eq(Product::getAvailable, Boolean.TRUE);
        return getBaseMapper().selectList(checkWrapper);
    }

    private List<Product> queryAvailableProductsByNames(Set<String> names) {
        if (CollectionUtil.isEmpty(names)) {
            return CollectionUtil.emptyList();
        }

        Wrapper<Product> checkWrapper = Wrappers.lambdaQuery(Product.class)
                .in(Product::getName, names)
                .eq(Product::getAvailable, Boolean.TRUE);
        return getBaseMapper().selectList(checkWrapper);
    }

    private List<Product> queryAvailableProductsByName(String name) {
        Wrapper<Product> checkWrapper = Wrappers.lambdaQuery(Product.class)
                .apply("TRIM(name) = {0}", Objects.toString(name, "").trim())
                .eq(Product::getAvailable, Boolean.TRUE);
        return getBaseMapper().selectList(checkWrapper);
    }

    private Set<String> queryCodesByPrefix(String prefix) {
        Wrapper<Product> checkWrapper = Wrappers.lambdaQuery(Product.class)
                .likeRight(Product::getCode, prefix)
                .eq(Product::getAvailable, Boolean.TRUE);
        return getBaseMapper().selectList(checkWrapper).stream()
                .map(Product::getCode)
                .filter(StringUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    private boolean existsProductCode(String code) {
        Wrapper<Product> checkWrapper = Wrappers.lambdaQuery(Product.class).eq(Product::getCode, code)
                .eq(Product::getAvailable, Boolean.TRUE);
        return getBaseMapper().selectCount(checkWrapper) > 0;
    }

    /**
     * 单位配置以商品的 unit 为主单位。历史单据保存单位快照，因此这里只影响后续业务。
     */
    private void saveUnits(Product product, List<ProductUnitVo> unitVos) {
        String baseUnitId = product.getUnit();
        Unit baseUnitRecord = StringUtil.isBlank(baseUnitId) ? null : unitService.getById(baseUnitId);
        String baseUnit = baseUnitRecord == null ? null : baseUnitRecord.getName();
        if (CollectionUtil.isEmpty(unitVos)) {
            if (StringUtil.isBlank(baseUnit)) {
                return;
            }
            unitVos = new ArrayList<>();
            ProductUnitVo unitVo = new ProductUnitVo();
            unitVo.setUnitName(baseUnit);
            unitVo.setConversionRate(BigDecimal.ONE);
            unitVos.add(unitVo);
        }
        if (StringUtil.isBlank(baseUnit)) {
            throw new DefaultClientException("主单位不存在或已停用！");
        }
        Set<String> unitNames = new HashSet<>();
        boolean hasBaseUnit = false;
        for (ProductUnitVo unitVo : unitVos) {
            if (!unitNames.add(unitVo.getUnitName())) {
                throw new DefaultClientException("单位名称不能重复！");
            }
            if (unitVo.getConversionRate() == null || NumberUtil.le(unitVo.getConversionRate(), BigDecimal.ZERO)
                    || !NumberUtil.isNumberPrecision(unitVo.getConversionRate(), 6)) {
                throw new DefaultClientException("单位换算率必须大于0，且最多6位小数！");
            }
            if (baseUnit.equals(unitVo.getUnitName())) {
                if (!NumberUtil.equal(unitVo.getConversionRate(), BigDecimal.ONE)) {
                    throw new DefaultClientException("主单位换算率必须为1！");
                }
                hasBaseUnit = true;
            }
        }
        if (!hasBaseUnit) {
            throw new DefaultClientException("多单位配置必须包含主单位！");
        }
        productUnitService.remove(Wrappers.lambdaQuery(ProductUnit.class)
                .eq(ProductUnit::getProductId, product.getId()));
        List<ProductUnit> units = new ArrayList<>();
        for (int i = 0; i < unitVos.size(); i++) {
            ProductUnitVo unitVo = unitVos.get(i);
            ProductUnit unit = new ProductUnit();
            unit.setId(IdUtil.getId());
            unit.setProductId(product.getId());
            unit.setUnitName(unitVo.getUnitName());
            unit.setConversionRate(unitVo.getConversionRate());
            unit.setBaseUnit(baseUnit.equals(unitVo.getUnitName()));
            unit.setAvailable(!Boolean.FALSE.equals(unitVo.getAvailable()));
            unit.setSortNo(unitVo.getSortNo() == null ? i : unitVo.getSortNo());
            units.add(unit);
        }
        productUnitService.saveBatch(units);
    }

    private String buildNameSpecUnitKey(ProductImportModel data) {
        return buildNameSpecUnitKey(data.getName(), data.getSpec(), data.getUnit());
    }

    private String buildNameSpecUnitKey(CreateProductVo data) {
        return buildNameSpecUnitKey(data.getName(), data.getSpec(), data.getUnit());
    }

    private String buildNameSpecUnitKey(Product data) {
        return buildNameSpecUnitKey(data.getName(), data.getSpec(), data.getUnit());
    }

    private String buildNameSpecUnitKey(String name, String spec, String unit) {
        return Objects.toString(name, "").trim() + "||" + Objects.toString(spec, "").trim() + "||"
                + Objects.toString(unit, "").trim();
    }

    private void checkNameSpecUnit(CreateProductVo vo) {
        String key = buildNameSpecUnitKey(vo);
        List<Product> products = queryAvailableProductsByName(vo.getName());
        boolean exists = products.stream()
                .map(this::buildNameSpecUnitKey)
                .anyMatch(key::equals);
        if (exists) {
            throw new DefaultClientException("名称+规格+单位重复，请重新输入！");
        }
    }
}
