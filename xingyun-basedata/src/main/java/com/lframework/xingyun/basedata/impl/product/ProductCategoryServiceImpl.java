package com.lframework.xingyun.basedata.impl.product;

import cn.hutool.core.date.StopWatch;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lframework.starter.common.constants.PatternPool;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.ObjectUtil;
import com.lframework.starter.common.utils.RegUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.annotations.oplog.OpLog;
import com.lframework.starter.web.core.event.DataChangeEventBuilder;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.core.utils.OpLogUtil;
import com.lframework.starter.web.inner.entity.RecursionMapping;
import com.lframework.starter.web.inner.service.RecursionMappingService;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.ProductCategory;
import com.lframework.xingyun.basedata.enums.BaseDataOpLogType;
import com.lframework.xingyun.basedata.enums.ProductCategoryNodeType;
import com.lframework.xingyun.basedata.events.DeleteProductCategoryEvent;
import com.lframework.xingyun.basedata.excel.product.category.ProductCategoryImportModel;
import com.lframework.xingyun.basedata.mappers.ProductCategoryMapper;
import com.lframework.xingyun.basedata.service.product.ProductCategoryService;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.basedata.vo.product.category.CreateProductCategoryVo;
import com.lframework.xingyun.basedata.vo.product.category.QueryProductCategorySelectorVo;
import com.lframework.xingyun.basedata.vo.product.category.UpdateProductCategoryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductCategoryServiceImpl extends
        BaseMpServiceImpl<ProductCategoryMapper, ProductCategory>
        implements ProductCategoryService {

    @Autowired
    private RecursionMappingService recursionMappingService;

    @Autowired
    private ProductService productService;

    @Override
    public List<ProductCategory> getAllProductCategories() {

        return getBaseMapper().getAllProductCategories();
    }

    @Cacheable(value = ProductCategory.CACHE_NAME, key = "@cacheVariables.tenantId() + #id", unless = "#result == null")
    @Override
    public ProductCategory findById(String id) {

        return getBaseMapper().selectById(id);
    }

    @Override
    public List<ProductCategory> selector(QueryProductCategorySelectorVo vo) {

        return getBaseMapper().selector(vo);
    }

    @OpLog(type = BaseDataOpLogType.class, name = "删除商品分类，ID：{}", params = "#id")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(String id) {

        List<String> batchIds = new ArrayList<>();
        batchIds.add(id);
        List<String> nodeChildIds = recursionMappingService.getNodeChildIds(id,
                ProductCategoryNodeType.class);
        if (CollectionUtil.isNotEmpty(nodeChildIds)) {
            batchIds.addAll(nodeChildIds);
        }

        Wrapper<ProductCategory> updateWrapper = Wrappers.lambdaUpdate(ProductCategory.class)
                .set(ProductCategory::getAvailable, Boolean.FALSE).in(ProductCategory::getId, batchIds);
        getBaseMapper().update(updateWrapper);

        for (String categoryId : batchIds) {
            ProductCategory category = this.findById(categoryId);

            DataChangeEventBuilder.publishLogicDelete(this, DeleteProductCategoryEvent.class, category);
        }
    }

    @OpLog(type = BaseDataOpLogType.class, name = "新增商品分类，ID：{}, 编号：{}", params = {"#id",
            "#code"})
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String create(CreateProductCategoryVo vo) {

        // 查询Code是否重复
        Wrapper<ProductCategory> checkCodeWrapper = Wrappers.lambdaQuery(ProductCategory.class)
                .eq(ProductCategory::getCode, vo.getCode()).eq(ProductCategory::getAvailable, Boolean.TRUE);
        if (getBaseMapper().selectCount(checkCodeWrapper) > 0) {
            throw new DefaultClientException("编号重复，请重新输入！");
        }

        // 查询Name是否重复
        Wrapper<ProductCategory> checkNameWrapper = Wrappers.lambdaQuery(ProductCategory.class)
                .eq(ProductCategory::getName, vo.getName()).eq(ProductCategory::getAvailable, Boolean.TRUE);
        if (getBaseMapper().selectCount(checkNameWrapper) > 0) {
            throw new DefaultClientException("名称重复，请重新输入！");
        }

        // 如果parentId不为空，查询上级分类是否存在
        if (!StringUtil.isBlank(vo.getParentId())) {
            Wrapper<ProductCategory> checkParentWrapper = Wrappers.lambdaQuery(ProductCategory.class)
                    .eq(ProductCategory::getId, vo.getParentId())
                    .eq(ProductCategory::getAvailable, Boolean.TRUE);
            if (getBaseMapper().selectCount(checkParentWrapper) == 0) {
                throw new DefaultClientException("上级分类不存在，请检查！");
            }

            // 然后判断上级分类下是否有商品，如果有商品不允许新增子分类
            Wrapper<Product> checkProductWrapper = Wrappers.lambdaQuery(Product.class)
                    .eq(Product::getCategoryId, vo.getParentId())
                    .eq(Product::getAvailable, Boolean.TRUE);
            if (productService.count(checkProductWrapper) > 0) {
                throw new DefaultClientException("上级分类已关联商品，不允许新增子分类！");
            }
        }

        ProductCategory data = new ProductCategory();
        data.setId(IdUtil.getId());
        data.setCode(vo.getCode());
        data.setName(vo.getName());
        if (!StringUtil.isBlank(vo.getParentId())) {
            data.setParentId(vo.getParentId());
        }
        data.setAvailable(Boolean.TRUE);
        data.setDescription(vo.getDescription());

        getBaseMapper().insert(data);

        ProductCategory productCategory = new ProductCategory();
        productCategory.setId(data.getId());
        productCategory.setParentId(data.getParentId());
        List<ProductCategory> categoryList = Lists.newArrayList(productCategory);
        this.saveRecursion(true, categoryList);

        OpLogUtil.setVariable("id", data.getId());
        OpLogUtil.setVariable("code", vo.getCode());
        OpLogUtil.setExtra(vo);

        return data.getId();
    }

    @OpLog(type = BaseDataOpLogType.class, name = "修改商品分类，ID：{}, 编号：{}", params = {"#id",
            "#code"})
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(UpdateProductCategoryVo vo) {

        ProductCategory data = getBaseMapper().selectById(vo.getId());
        if (ObjectUtil.isNull(data)) {
            throw new DefaultClientException("分类不存在！");
        }

        // 查询Code是否重复
        Wrapper<ProductCategory> checkCodeWrapper = Wrappers.lambdaQuery(ProductCategory.class)
                .eq(ProductCategory::getCode, vo.getCode()).eq(ProductCategory::getAvailable, Boolean.TRUE)
                .ne(ProductCategory::getId, data.getId());
        if (getBaseMapper().selectCount(checkCodeWrapper) > 0) {
            throw new DefaultClientException("编号重复，请重新输入！");
        }

        // 查询Name是否重复
        Wrapper<ProductCategory> checkNameWrapper = Wrappers.lambdaQuery(ProductCategory.class)
                .eq(ProductCategory::getName, vo.getName())
                .eq(ProductCategory::getAvailable, Boolean.TRUE)
                .ne(ProductCategory::getId, data.getId());
        if (getBaseMapper().selectCount(checkNameWrapper) > 0) {
            throw new DefaultClientException("名称重复，请重新输入！");
        }

        Wrapper<ProductCategory> updateWrapper = Wrappers.lambdaUpdate(ProductCategory.class)
                .set(ProductCategory::getCode, vo.getCode()).set(ProductCategory::getName, vo.getName())
                .set(ProductCategory::getDescription,
                        StringUtil.isBlank(vo.getDescription()) ? StringPool.EMPTY_STR : vo.getDescription())
                .eq(ProductCategory::getId, data.getId());

        getBaseMapper().update(updateWrapper);

        OpLogUtil.setVariable("id", data.getId());
        OpLogUtil.setVariable("code", vo.getCode());
        OpLogUtil.setExtra(vo);
    }

    /**
     * 保存递归信息
     *
     * @param categoryList
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveRecursion(Boolean isCreate, List<ProductCategory> categoryList) {
        if (CollectionUtil.isEmpty(categoryList)) {
            return;
        }
        List<String> categoryIdList = categoryList.stream().map(ProductCategory::getId).collect(Collectors.toList());
        if (!isCreate) {
            recursionMappingService.deleteBatchNode(categoryIdList, ProductCategoryNodeType.class);
        }

        Map<String, RecursionMapping> recursionMappingMap = recursionMappingService.selectAll(ProductCategoryNodeType.class)
                .stream()
                .collect(Collectors.toMap(RecursionMapping::getNodeId, r -> r));

        // 组装新的节点关系
        List<RecursionMapping> list = Lists.newArrayList();
        List<String> deleteNodeIdList = Lists.newArrayList();

        addData(categoryList, recursionMappingMap, list);

        updateChildNode(recursionMappingMap, categoryIdList, deleteNodeIdList, list);

        // 持久化
        if (CollectionUtil.isNotEmpty(deleteNodeIdList)) {
            recursionMappingService.deleteBatchNode(deleteNodeIdList, ProductCategoryNodeType.class);
        }
        recursionMappingService.batchSaveNode(list, ProductCategoryNodeType.class);
    }

    /**
     List<String> deleteNodeIdList = Lists.newArrayList();
     *
     * @param map
     * @param categoryIdList
     * @param deleteNodeIdList
     * @param list
     */
    private void updateChildNode(Map<String, RecursionMapping> map, List<String> categoryIdList, List<String> deleteNodeIdList, List<RecursionMapping> list) {
        // 还要更新这个节点的子节点
        // 节点对应子节点Map
        Map<String, List<String>> childMap = Maps.newHashMap();
        map.values().forEach(item -> {
            if (StringUtil.isEmpty(item.getPath())) {
                return;
            }
            Lists.newArrayList(item.getPath().split(StringPool.STR_SPLIT)).forEach(path -> {
                List<String> orDefault = childMap.getOrDefault(path, Lists.newArrayList());
                orDefault.add(item.getNodeId());
                childMap.put(path, orDefault);
            });
        });
        // 查询所有的分类
        Map<String, ProductCategory> categoryMap = getAllProductCategories().stream().collect(Collectors.toMap(ProductCategory::getId, r -> r));
        categoryIdList.forEach(item -> {
            List<String> childIds = childMap.get(item);
            if (CollectionUtil.isEmpty(childIds)) {
                return;
            }
            for (String childId : childIds) {
                List<ProductCategory> parentDeptList = new ArrayList<>();
                ProductCategory productCategory = categoryMap.get(childId);

                while (StringUtil.isNotBlank(productCategory.getParentId())) {
                    productCategory = categoryMap.get(productCategory.getParentId());
                    if (productCategory == null) {
                        break;
                    }
                    parentDeptList.add(productCategory);
                }

                parentDeptList = CollectionUtil.reverse(parentDeptList);
                deleteNodeIdList.add(childId);

                RecursionMapping recursionMapping = new RecursionMapping();
                recursionMapping.setNodeId(childId);
                recursionMapping.setPath(StringUtil.join(StringPool.STR_SPLIT, parentDeptList.stream().map(ProductCategory::getId).collect(Collectors.toList())));
                list.add(recursionMapping);
            }
        });
    }

    /**
     * 组装新的数据
     * @param categoryList
     * @param map
     * @param list
     */
    private void addData(List<ProductCategory> categoryList, Map<String, RecursionMapping> map, List<RecursionMapping> list) {
        categoryList.forEach(item -> {
            RecursionMapping newRecursionMapping = new RecursionMapping();
            newRecursionMapping.setNodeId(item.getId());

            // 有父节点
            if (StringUtil.isNotBlank(item.getParentId())) {
                RecursionMapping parent = map.get(item.getParentId());
                if (parent == null || StringUtil.isEmpty(parent.getPath())) {
                    newRecursionMapping.setPath(item.getParentId());
                } else {
                    newRecursionMapping.setPath(parent.getPath() + StringPool.STR_SPLIT + item.getParentId());
                }
            }
            list.add(newRecursionMapping);
        });
    }

    @CacheEvict(value = ProductCategory.CACHE_NAME, key = "@cacheVariables.tenantId() + #key")
    @Override
    public void cleanCacheByKey(Serializable key) {

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void importExcel(List<ProductCategoryImportModel> list) {
        if (CollectionUtil.isEmpty(list)) {
            throw new DefaultClientException("导入数据为空！");
        }

        StopWatch stopWatch = new StopWatch("导入商品分类");
        stopWatch.start("校验");
        List<ProductCategory> persists = Lists.newArrayList();
        checkCategory(list, persists);
        stopWatch.stop();

        stopWatch.start("持久化insertBatch");
        saveBatch(persists, 100);
        stopWatch.stop();

        stopWatch.start("持久化saveRecursion");
        saveRecursion(true, persists);

        stopWatch.stop();
        log.info(stopWatch.prettyPrint(TimeUnit.MILLISECONDS));
    }

    /**
     * 规则校验
     * @param list
     * @param persists
     */
    private void checkCategory(List<ProductCategoryImportModel> list, List<ProductCategory> persists) {
        List<String> checkList = Lists.newArrayList();
        // 校验编号是否已经存在
        Map<String, ProductCategory> key2categoryMap = this.getAllProductCategories().stream()
                .collect(Collectors.toMap(ProductCategory::getCode, Function.identity()));

        Map<String, ProductCategory> parent2categoryMap = this.getAllProductCategories().stream()
                .collect(Collectors.toMap(ProductCategory::getParentId, Function.identity(), (v1, v2) -> v2));

        Map<String, ProductCategory> name2categoryMap = this.getAllProductCategories().stream()
                .collect(Collectors.toMap(ProductCategory::getName, Function.identity()));


        // 规则校验
        for (int i = 0; i < list.size(); i++) {
            ProductCategoryImportModel data = list.get(i);
            int rowIndex = (i + 2);
            if (StringUtil.isBlank(data.getCode())) {
                throw new DefaultClientException(String.format("第%s行“编号”不能为空", rowIndex));
            }
            if (StringUtil.isBlank(data.getName())) {
                throw new DefaultClientException(String.format("第%s行“名称”不能为空", rowIndex));
            }

            if (!RegUtil.isMatch(PatternPool.PATTERN_CODE, data.getCode())) {
                throw new DefaultClientException(
                        String.format("第%s行“编号”必须由字母、数字、“-_.”组成，长度不能超过20位", rowIndex));
            }
            if (checkList.contains(data.getCode())) {
                throw new DefaultClientException(String.format(
                        "第%s行“编号”与第%s行重复", rowIndex, (checkList.indexOf(data.getCode()) + 1)));
            }
            checkList.add(data.getCode());
            if (key2categoryMap.containsKey(data.getCode())) {
                throw new DefaultClientException(String.format("第%s行“编号”已存在", rowIndex));
            }
            if (name2categoryMap.containsKey(data.getName())) {
                throw new DefaultClientException(String.format("第%s行“名称”已存在", rowIndex));
            }

            ProductCategory record = new ProductCategory();

            if (!StringUtil.isBlank(data.getParentCode())) {
                ProductCategory parent = key2categoryMap.get(data.getParentCode());
                if (parent == null) {
                    // 检查是不是新导入的
                    if (list.stream().noneMatch(t -> t.getCode().equals(data.getParentCode()))) {
                        throw new DefaultClientException(String.format("第%s行“上级分类编号”不存在", rowIndex));
                    }
                } else {
                    record.setParentId(parent.getId());
                }

                ProductCategory productCategory = key2categoryMap.get(data.getCode());
                if (productCategory != null) {
                    ProductCategory parentCategory = StringUtil.isBlank(productCategory.getParentId()) ? null
                            : parent2categoryMap.get(productCategory.getParentId());
                    if (parentCategory == null || !parentCategory.getCode().equals(data.getParentCode())) {
                        throw new DefaultClientException(
                                String.format("第%s行“上级分类编号”有误，不允许修改分类的归属关系", rowIndex));
                    }
                }
            }

            record.setId(IdUtil.getId());
            record.setCode(data.getCode());
            record.setName(data.getName());
            record.setDescription(data.getDescription() == null ? "" : data.getDescription());
            record.setAvailable(Boolean.TRUE);
            persists.add(record);

            data.setId(record.getId());
        }
    }
}
