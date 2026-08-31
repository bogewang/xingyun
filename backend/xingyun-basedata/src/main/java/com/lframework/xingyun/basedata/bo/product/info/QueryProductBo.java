package com.lframework.xingyun.basedata.bo.product.info;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.web.core.annotations.convert.EnumConvert;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.Unit;
import com.lframework.xingyun.basedata.service.UnitService;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QueryProductBo extends BaseBo<Product> {

    /**
     * ID
     */
    @ApiModelProperty("ID")
    private String id;

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
     * 名称
     */
    @ApiModelProperty("简称")
    private String shortName;

    /**
     * SKU
     */
    @ApiModelProperty("SKU")
    private String skuCode;

    /**
     * 单位
     */
    @ApiModelProperty("单位")
    private String unit;

    /**
     * 规格
     */
    @ApiModelProperty("规格")
    private String spec;

    /**
     * 库存数量
     */
    @ApiModelProperty("库存数量")
    private Integer stockNum;

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
     * 零售价
     */
    @ApiModelProperty("零售价")
    private BigDecimal retailPrice;

    /**
     * 商品启用状态。
     */
    @ApiModelProperty("商品启用状态")
    private Boolean available;

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
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = StringPool.DATE_TIME_PATTERN)
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @ApiModelProperty("修改时间")
    @JsonFormat(pattern = StringPool.DATE_TIME_PATTERN)
    private LocalDateTime updateTime;

    public QueryProductBo() {

    }

    public QueryProductBo(Product dto) {

        super(dto);
    }

    @Override
    protected void afterInit(Product dto) {
        Unit unit = ApplicationUtil.getBean(UnitService.class).getById(dto.getUnit());
        this.unit = unit == null ? dto.getUnit() : unit.getName();
    }
}
