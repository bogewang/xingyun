package com.lframework.xingyun.basedata.excel.product;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.annotations.excel.ExcelRequired;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.ProductCategory;
import com.lframework.xingyun.basedata.service.product.ProductCategoryService;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductImportModel extends BaseBo<Product> implements ExcelModel {

    /**
     * ID
     */
    @ExcelIgnore
    private String id;

    /**
     * 名称
     */
    @ExcelRequired
    @ExcelProperty("名称")
    private String name;
    /**
     * 简称
     */
    @ExcelProperty("简称")
    private String shortName;



    /**
     * 规格
     */
    @ExcelProperty("规格")
    private String spec;

    /**
     * 分类ID
     */
    @ExcelIgnore
    private String categoryId;

    /**
     * 分类编号
     */
    @ExcelRequired
    @ExcelProperty("商品分类")
    private String categoryName;



    /**
     * 单位
     */
    @ExcelProperty("单位")
    @ExcelRequired
    private String unit;

    /**
     * 采购价
     */
    // @ExcelRequired
    @ExcelProperty("采购价（元）")
    private BigDecimal purchasePrice;

    /**
     * 销售价
     */
    @ExcelProperty("销售价（元）")
    private BigDecimal salePrice;

    /**
     * 零售价
     */
    @ExcelProperty("零售价（元）")
    private BigDecimal retailPrice;

    /**
     * 别名
     */
    @ExcelProperty("别名")
    private String alias;

    /**
     * 备注
     */
    @ExcelProperty("备注")
    private String remark;
    /**
     * 备注二
     */
    @ExcelProperty("备注2")
    private String remark2;

    /**
     * 毛利
     */
    @ExcelProperty("毛利")
    private String profit;

    /**
     * 供应商
     */
    @ExcelProperty("供应商")
    private String supplier;



    /**
     * 编号
     */
    // @ExcelRequired
    @ExcelProperty("商品编码")
    private String code;

    /**
     * SKU编号
     */
    @ExcelProperty("SKU编号")
    private String skuCode;
    /**
     * 简码
     */
    @ExcelProperty("简码")
    private String externalCode;

    /**
     * 品牌编号
     */
    @ExcelProperty("品牌编号")
    private String brandCode;

    /**
     * 进项税率（%）
     */
    @ExcelProperty("进项税率（%）")
    private BigDecimal taxRate;

    /**
     * 销项税率（%）
     */
    @ExcelProperty("销项税率（%）")
    private BigDecimal saleTaxRate;

    /**
     * 品牌ID
     */
    @ExcelIgnore
    private String brandId;

    public ProductImportModel() {
    }

    public ProductImportModel(Product dto) {
        super(dto);
    }

    @Override
    protected void afterInit(Product dto) {
        ProductCategoryService productCategoryService =  ApplicationUtil.getBean(ProductCategoryService.class);
        if (dto.getCategoryId() != null) {
            ProductCategory category = productCategoryService.findById(dto.getCategoryId());
            this.categoryName = category.getName();
        }
    }


}
