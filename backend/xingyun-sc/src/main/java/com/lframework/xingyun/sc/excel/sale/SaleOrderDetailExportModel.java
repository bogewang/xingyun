package com.lframework.xingyun.sc.excel.sale;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.ProductCategory;
import com.lframework.xingyun.basedata.service.product.ProductCategoryService;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.sc.entity.SaleOrderDetail;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaleOrderDetailExportModel extends BaseBo<SaleOrderDetail> implements ExcelModel {

  /**
   * 商品编号
   */
  @ExcelProperty("商品编号")
  private String productCode;

  /**
   * 名称
   */
  @ExcelProperty("名称")
  private String productName;
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
   * 单位
   */
  @ExcelProperty("单位")
  private String unit;
  /**
   * 商品分类
   */
  @ExcelProperty("商品分类")
  private String categoryName;
  /**
   * 价格（元）
   */
  @ExcelProperty("价格（元）")
  private BigDecimal taxPrice;
  /**
   * 销售数量
   */
  @ExcelProperty("销售数量")
  private BigDecimal orderNum;
  /**
   * 含税金额
   */
  @ExcelProperty("含税金额")
  private BigDecimal taxAmount;
  /**
   * 备注
   */
  @ExcelProperty("备注")
  private String description;

  public SaleOrderDetailExportModel() {

  }

  public SaleOrderDetailExportModel(SaleOrderDetail dto) {

    super(dto);
  }

  @Override
  public <A> BaseBo<SaleOrderDetail> convert(SaleOrderDetail dto) {

    return this;
  }

  @Override
  protected void afterInit(SaleOrderDetail dto) {

    ProductService productService = ApplicationUtil.getBean(ProductService.class);
    Product product = productService.findById(dto.getProductId());

    ProductCategoryService productCategoryService = ApplicationUtil.getBean(ProductCategoryService.class);
    ProductCategory productCategory = productCategoryService.findById(product.getCategoryId());

    this.setProductCode(product.getCode());
    this.setProductName(product.getName());
    this.setShortName(product.getShortName());
    this.setSpec(product.getSpec());
    this.setUnit(product.getUnit());
    this.setCategoryName(productCategory.getName());
    this.setTaxPrice(dto.getTaxPrice());
    this.setOrderNum(dto.getOrderNum());
    this.setTaxAmount(dto.getTaxAmount());
    this.setDescription(dto.getDescription());
  }
}
