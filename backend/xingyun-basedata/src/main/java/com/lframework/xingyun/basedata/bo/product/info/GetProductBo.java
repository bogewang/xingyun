package com.lframework.xingyun.basedata.bo.product.info;

import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.annotations.convert.EnumConvert;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.dto.product.ProductPropertyRelationDto;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.ProductBrand;
import com.lframework.xingyun.basedata.entity.ProductCategory;
import com.lframework.xingyun.basedata.entity.ProductUnit;
import com.lframework.xingyun.basedata.entity.Supplier;
import com.lframework.xingyun.basedata.enums.ColumnType;
import com.lframework.xingyun.basedata.service.product.ProductBrandService;
import com.lframework.xingyun.basedata.service.product.ProductCategoryService;
import com.lframework.xingyun.basedata.service.product.ProductPropertyRelationService;
import com.lframework.xingyun.basedata.service.product.ProductUnitService;
import com.lframework.xingyun.basedata.service.supplier.SupplierService;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class GetProductBo extends BaseBo<Product> {

  /**
   * ID
   */
  @ApiModelProperty("ID")
  private String id;

  /**
   * 商品启用状态，用于历史业务数据回显。
   */
  @ApiModelProperty("商品启用状态")
  private Boolean available;

  /**
   * 编号
   */
  @ApiModelProperty("编号")
  private String code;

  /**
   * 名称
   */
  @ApiModelProperty("名称")
  private String name;

  /**
   * 简称
   */
  @ApiModelProperty("简称")
  private String shortName;

  /**
   * SKU
   */
  @ApiModelProperty("SKU")
  private String skuCode;

  /**
   * 简码
   */
  @ApiModelProperty("简码")
  private String externalCode;

  /**
   * 分类ID
   */
  @ApiModelProperty("分类ID")
  private String categoryId;

  /**
   * 分类名称
   */
  @ApiModelProperty("分类名称")
  private String categoryName;

  /**
   * 品牌ID
   */
  @ApiModelProperty("品牌ID")
  private String brandId;

  /**
   * 品牌名称
   */
  @ApiModelProperty("品牌名称")
  private String brandName;

  /**
   * 重量（kg）
   */
  @ApiModelProperty("重量（kg）")
  private BigDecimal weight;

  /**
   * 体积（cm3）
   */
  @ApiModelProperty("体积（cm3）")
  private BigDecimal volume;

  /**
   * 进项税率（%）
   */
  @ApiModelProperty("进项税率（%）")
  private BigDecimal taxRate;

  /**
   * 销项税率（%）
   */
  @ApiModelProperty("销项税率（%）")
  private BigDecimal saleTaxRate;

  /**
   * 规格
   */
  @ApiModelProperty("规格")
  private String spec;

  /**
   * 单位
   */
  @ApiModelProperty("单位")
  private String unit;

  @ApiModelProperty("商品多单位")
  private List<ProductUnit> units;

  /**
   * 采购价
   */
  @ApiModelProperty("采购价")
  private BigDecimal purchasePrice;

  /**
   * 零售价
   */
  @ApiModelProperty("零售价")
  private BigDecimal retailPrice;

  @ApiModelProperty("别名")
  private String alias;

  @ApiModelProperty("默认供应商")
  private String defaultSupplier;

  @ApiModelProperty("默认供应商名称")
  private String defaultSupplierName;

  @ApiModelProperty("备注")
  private String remark;

  @ApiModelProperty("备注二")
  private String remark2;

  /**
   * 属性
   */
  @ApiModelProperty("属性")
  private List<PropertyBo> properties;

  public GetProductBo() {

  }

  public GetProductBo(Product dto) {

    super(dto);
  }

  @Override
  public BaseBo<Product> convert(Product dto) {

    return super.convert(dto, GetProductBo::getProperties);
  }

  @Override
  protected void afterInit(Product dto) {

    ProductCategoryService productCategoryService = ApplicationUtil.getBean(
        ProductCategoryService.class);
    ProductCategory productCategory = productCategoryService.findById(dto.getCategoryId());
    this.categoryName = productCategory.getName();

    if (StringUtil.isNotBlank(dto.getBrandId())) {
      ProductBrandService productBrandService = ApplicationUtil.getBean(ProductBrandService.class);
      ProductBrand productBrand = productBrandService.findById(dto.getBrandId());
      this.brandName = productBrand.getName();
    }

    this.purchasePrice = dto.getPurchasePrice();
    ProductUnitService productUnitService = ApplicationUtil.getBean(ProductUnitService.class);
    this.units = productUnitService.getByProductId(dto.getId());
    this.retailPrice = dto.getRetailPrice();
    this.alias = dto.getAlias();
    this.defaultSupplier = dto.getDefaultSupplier();
    if (StringUtil.isNotBlank(dto.getDefaultSupplier())) {
      SupplierService supplierService = ApplicationUtil.getBean(SupplierService.class);
      Supplier supplier = supplierService.findById(dto.getDefaultSupplier());
      if (supplier != null) {
        this.defaultSupplierName = supplier.getName();
      }
    }
    this.remark = dto.getRemark();
    this.remark2 = dto.getRemark2();

    ProductPropertyRelationService productPropertyRelationService = ApplicationUtil.getBean(
        ProductPropertyRelationService.class);
    List<ProductPropertyRelationDto> propertyRelationDtos = productPropertyRelationService.getByProductId(
        dto.getId());
    if (!CollectionUtil.isEmpty(propertyRelationDtos)) {
      this.properties = new ArrayList<>();
      for (ProductPropertyRelationDto property : propertyRelationDtos) {
        if (property.getPropertyColumnType() == ColumnType.MULTIPLE) {
          PropertyBo propertyBo = this.properties.stream()
              .filter(t -> t.getId().equals(property.getPropertyId())).findFirst().orElse(null);
          if (propertyBo == null) {
            this.properties.add(new PropertyBo(property));
          } else {
            propertyBo.setText(propertyBo.getText().concat(StringPool.STR_SPLIT)
                .concat(property.getPropertyItemId()));
            propertyBo.setTextStr(propertyBo.getTextStr().concat(StringPool.STR_SPLIT_CN)
                .concat(property.getPropertyText()));
          }
        } else {
          this.properties.add(new PropertyBo(property));
        }
      }
    }
  }

  @Data
  public static class PropertyBo extends BaseBo<ProductPropertyRelationDto> {

    /**
     * 属性ID
     */
    @ApiModelProperty("属性ID")
    private String id;

    /**
     * 属性名
     */
    @ApiModelProperty("属性名")
    private String name;

    /**
     * 字段类型
     */
    @ApiModelProperty("字段类型")
    private Integer columnType;

    /**
     * 属性值
     */
    @ApiModelProperty("属性值")
    private String text;

    /**
     * 属性文本
     */
    @ApiModelProperty("属性文本")
    private String textStr;

    public PropertyBo() {

    }

    public PropertyBo(ProductPropertyRelationDto dto) {

      super(dto);
    }

    @Override
    public BaseBo<ProductPropertyRelationDto> convert(ProductPropertyRelationDto dto) {

      return super.convert(dto, PropertyBo::getColumnType);
    }

    @Override
    protected void afterInit(ProductPropertyRelationDto dto) {

      this.id = dto.getPropertyId();
      this.name = dto.getPropertyName();
      this.text = dto.getPropertyColumnType() == ColumnType.CUSTOM ? dto.getPropertyText()
          : dto.getPropertyItemId();
      this.textStr = dto.getPropertyText();
      this.columnType = dto.getPropertyColumnType().getCode();
    }
  }
}
