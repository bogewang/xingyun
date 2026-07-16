package com.lframework.xingyun.sc.vo.sale.out;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SaleOutProductVo implements BaseVo, Serializable {

    private static final long serialVersionUID = 1L;
    private Integer seq;
    /**
     * 商品ID
     */
    @ApiModelProperty("商品ID")
    private String productId;
    /**
     * 商品编号
     */
    @ApiModelProperty("商品编号")
    private String productCode;
    /**
     * 商品名称
     */
    @ApiModelProperty("商品名称")
    private String productName;
    /**
     * 商品规格
     */
    @ApiModelProperty("商品规格")
    private String spec;

    /**
     * 商品单位
     */
    @ApiModelProperty("商品单位")
    private String unit;

    @ApiModelProperty("商品单位ID")
    private String unitId;

    /**
     * 原价
     */
    @ApiModelProperty("原价")
    private BigDecimal oriPrice;

    /**
     * 现价
     */
    @ApiModelProperty("现价")
    private BigDecimal taxPrice;

    /**
     * 折扣（%）
     */
    @ApiModelProperty("折扣（%）")
    private BigDecimal discountRate;

    /**
     * 出库数量
     */
    @ApiModelProperty("出库数量")
    private BigDecimal orderNum;

    /**
     * 备注
     */
    @ApiModelProperty("备注")
    private String description;

    /**
     * 成本单价
     */
    @ApiModelProperty("成本单价")
    private BigDecimal costPrice;

    /**
     * 销售订单明细ID
     */
    @ApiModelProperty("销售订单明细ID")
    private String saleOrderDetailId;

    /**
     * 配送日期
     */
    @ApiModelProperty("配送日期")
    private LocalDate actualDate;

    public BigDecimal getOrderNum() {
        return orderNum == null ? BigDecimal.ZERO : orderNum;
    }

    public BigDecimal getTaxPrice() {
        return taxPrice == null ? BigDecimal.ZERO : taxPrice;
    }
}
