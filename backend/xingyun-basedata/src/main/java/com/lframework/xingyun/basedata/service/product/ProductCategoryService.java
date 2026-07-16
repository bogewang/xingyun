package com.lframework.xingyun.basedata.service.product;

import com.lframework.starter.web.core.service.BaseMpService;
import com.lframework.xingyun.basedata.entity.ProductCategory;
import com.lframework.xingyun.basedata.excel.product.category.ProductCategoryImportModel;
import com.lframework.xingyun.basedata.vo.product.category.CreateProductCategoryVo;
import com.lframework.xingyun.basedata.vo.product.category.QueryProductCategorySelectorVo;
import com.lframework.xingyun.basedata.vo.product.category.UpdateProductCategoryVo;

import java.util.List;

public interface ProductCategoryService extends BaseMpService<ProductCategory> {

    /**
     * 查询全部分类信息
     *
     * @return
     */
    List<ProductCategory> getAllProductCategories();

    /**
     * 根据ID查询
     *
     * @param id
     * @return
     */
    ProductCategory findById(String id);

    /**
     * 选择器
     *
     * @return
     */
    List<ProductCategory> selector(QueryProductCategorySelectorVo vo);

    /**
     * 根据ID删除
     *
     * @param id
     */
    void deleteById(String id);

    /**
     * 创建
     *
     * @param vo
     * @return
     */
    String create(CreateProductCategoryVo vo);

    /**
     * 修改
     *
     * @param vo
     */
    void update(UpdateProductCategoryVo vo);

    /**
     * 保存关系
     *
     * @param isCreate
     * @param categoryList
     */
    void saveRecursion(Boolean isCreate, List<ProductCategory> categoryList);

    void importExcel(List<ProductCategoryImportModel> list);

    /**
     * 根据父级分类ID查询子分类
     *
     * @param parentIds 父级分类ID列表
     * @return 子分类列表
     */
    List<ProductCategory> getCategoryByParentIds(List<String> parentIds);

}
