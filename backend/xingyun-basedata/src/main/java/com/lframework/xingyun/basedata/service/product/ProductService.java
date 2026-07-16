package com.lframework.xingyun.basedata.service.product;

import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.service.BaseMpService;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.excel.product.ProductImportModel;
import com.lframework.xingyun.basedata.vo.product.info.CreateProductVo;
import com.lframework.xingyun.basedata.vo.product.info.QueryProductSelectorVo;
import com.lframework.xingyun.basedata.vo.product.info.QueryProductVo;
import com.lframework.xingyun.basedata.vo.product.info.UpdateProductVo;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService extends BaseMpService<Product> {

    /**
     * 查询列表
     *
     * @return
     */
    PageResult<Product> query(Integer pageIndex, Integer pageSize, QueryProductVo vo);

    /**
     * 查询列表
     *
     * @param vo
     * @return
     */
    List<Product> query(QueryProductVo vo);

    /**
     * 选择器
     *
     * @return
     */
    PageResult<Product> selector(Integer pageIndex, Integer pageSize, QueryProductSelectorVo vo);

    /**
     * 查询商品品种数
     *
     * @param vo
     * @return
     */
    Integer queryCount(QueryProductVo vo);

    /**
     * 根据ID查询
     *
     * @param id
     * @return
     */
    Product findById(String id);

    /**
     * 查询没有属性的ID
     *
     * @param propertyId
     * @return
     */
    List<String> getIdNotInProductProperty(String propertyId);

    /**
     * 根据分类ID查询
     *
     * @param categoryId
     * @return
     */
    List<String> getIdByCategoryId(String categoryId);

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
    String create(CreateProductVo vo);

    /**
     * 生成不重复的编号
     *
     * @return
     */
    String generateCode();

    /**
     * 修改
     *
     * @param vo
     */
    void update(UpdateProductVo vo);

    /**
     * 根据分类ID查询
     *
     * @param categoryIds
     * @return
     */
    List<Product> getByCategoryIds(List<String> categoryIds);

    /**
     * 根据品牌ID查询
     *
     * @param brandIds
     * @return
     */
    List<Product> getByBrandIds(List<String> brandIds);

    void importExcel(List<ProductImportModel> list);

    List<Product> selectAllAvailable();

    /**
     * 根据名称查询
     * @param productNames
     * @return
     */
    List<Product> selectByProductName(List<String> productNames);

    /**
     * 根据分类ID查询
     * @param ids
     * @return
     */
    List<Product> selectByIds(List<String> ids);

    void updatePrice(String id, BigDecimal salePrice, BigDecimal purchasePrice);
}
