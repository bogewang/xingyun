package com.lframework.xingyun.sc.excel.sale.out;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.ProductCategory;
import com.lframework.xingyun.basedata.service.product.ProductCategoryService;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaleOutSheetDetailExportModel extends BaseBo<SaleOutSheetDetail> implements ExcelModel {

  @ExcelProperty("商品编号")
  private String productCode;

  @ExcelProperty("名称")
  private String productName;

  @ExcelProperty("简称")
  private String shortName;

  @ExcelProperty("规格")
  private String spec;

  @ExcelProperty("单位")
  private String unit;

  @ExcelProperty("商品分类")
  private String categoryName;

  @ExcelProperty("价格（元）")
  private BigDecimal taxPrice;

  @ExcelProperty("出库数量")
  private BigDecimal orderNum;

  @ExcelProperty("含税金额")
  private BigDecimal taxAmount;

  @ExcelProperty("备注")
  private String description;

  public SaleOutSheetDetailExportModel() {
  }

  public SaleOutSheetDetailExportModel(SaleOutSheetDetail dto) {

    super(dto);
  }

  @Override
  public <A> BaseBo<SaleOutSheetDetail> convert(SaleOutSheetDetail dto) {

    return this;
  }

  @Override
  protected void afterInit(SaleOutSheetDetail dto) {

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
