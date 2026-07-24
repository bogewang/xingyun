package com.lframework.xingyun.sc.bo.sale;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.service.product.ProductLatestPriceCacheService;
import com.lframework.xingyun.basedata.entity.ProductUnit;
import com.lframework.xingyun.sc.dto.sale.SaleProductDto;
import com.lframework.xingyun.sc.entity.ProductStock;
import com.lframework.xingyun.sc.service.stock.ProductStockService;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class SaleProductBo extends BaseBo<SaleProductDto> {

    /**
     * ID
     */
    @ApiModelProperty("ID")
    private String productId;

    /**
     * 编号
     */
    @ApiModelProperty("编号")
    private String productCode;

    /**
     * 名称
     */
    @ApiModelProperty("名称")
    private String productName;

    /**
     * 热度星级
     */
    @ApiModelProperty("热度星级")
    private Integer hotLevel;

    /**
     * 简称
     */
    @ApiModelProperty("简称")
    private String shortName;

    /**
     * 分类名称
     */
    @ApiModelProperty("分类名称")
    private String categoryName;

    /**
     * 品牌名称
     */
    @ApiModelProperty("品牌名称")
    private String brandName;

    /**
     * 是否多销售属性
     */
    @ApiModelProperty("是否多销售属性")
    private Boolean multiSaleProp;

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
     * 规格
     */
    @ApiModelProperty("规格")
    private String spec;

    /**
     * 单位
     */
    @ApiModelProperty("单位")
    private String unit;

    /** 商品可选单位（含主单位）。 */
    @ApiModelProperty("商品可选单位")
    private List<ProductUnit> units;

    /**
     * 采购价
     */
    @ApiModelProperty("采购价")
    private BigDecimal purchasePrice;

    /**
     * 最新采购价
     */
    @ApiModelProperty("最新采购价")
    private BigDecimal latestPurchasePrice;

    /**
     * 销售价
     */
    @ApiModelProperty("销售价")
    private BigDecimal salePrice;

    /**
     * 最新销售价
     */
    @ApiModelProperty("最新销售价")
    private BigDecimal latestSalePrice;

    /**
     * 库存数量
     */
    @ApiModelProperty("库存数量")
    private BigDecimal stockNum;

    /**
     * 税率（%）
     */
    @ApiModelProperty("税率（%）")
    private BigDecimal taxRate;

    /**
     * 是否询价商品
     */
    @ApiModelProperty("是否询价商品")
    private Boolean inquiryProduct;

    /**
     * 仓库ID
     */
    @ApiModelProperty(value = "仓库ID", hidden = true)
    @JsonIgnore
    private String scId;

    public SaleProductBo(String scId, SaleProductDto dto) {

        this.scId = scId;
        this.init(dto);
    }

    @Override
    protected void afterInit(SaleProductDto dto) {

        this.productId = dto.getId();
        this.productCode = dto.getCode();
        this.productName = dto.getName();
        this.units = dto.getUnits();
        if (!CollectionUtil.isEmpty(this.units)) {
            this.unit = this.units.stream().filter(ProductUnit::getBaseUnit)
                .map(ProductUnit::getUnitName).findFirst().orElse(dto.getUnit());
        }
        ProductLatestPriceCacheService productLatestPriceCacheService = ApplicationUtil.getBean(ProductLatestPriceCacheService.class);
        this.latestSalePrice = productLatestPriceCacheService.getLatestSalePrice(this.getProductId());
        this.latestPurchasePrice = productLatestPriceCacheService.getLatestPurchasePrice(this.getProductId());

        ProductStockService productStockService = ApplicationUtil.getBean(
            ProductStockService.class);
        if (StringUtil.isBlank(this.getScId())) {
            List<ProductStock> productStocks = productStockService.list(
                Wrappers.lambdaQuery(ProductStock.class).eq(ProductStock::getProductId, this.getProductId()));
            if (CollectionUtil.isEmpty(productStocks)) {
                this.stockNum = BigDecimal.ZERO;
            } else {
                this.stockNum = productStocks.stream().map(ProductStock::getStockNum)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            return;
        }

        ProductStock productStock = productStockService.getByProductIdAndScId(this.getProductId(),
            this.getScId());
        this.stockNum = productStock == null ? BigDecimal.ZERO : productStock.getStockNum();
    }
}
