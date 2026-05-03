package com.lframework.xingyun.sc.bo.sale;

import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.xingyun.sc.dto.sale.SaleOrderFullDto;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PrintSaleOrderBo extends BaseBo<SaleOrderFullDto> {

    /**
     * 单号
     */
    @ApiModelProperty("单号")
    private String code;

    /**
     * 仓库编号
     */
    // @ApiModelProperty("仓库编号")
    // private String scCode;

    /**
     * 仓库名称
     */
    // @ApiModelProperty("仓库名称")
    // private String scName;

    /**
     * 客户编号
     */
    // @ApiModelProperty("客户编号")
    // private String customerCode;

    /**
     * 客户名称
     */
    @ApiModelProperty("客户名称")
    private String customerName;

    /**
     * 销售员姓名
     */
    // @ApiModelProperty("销售员姓名")
    // private String salerName;

    /**
     * 备注
     */
    @ApiModelProperty("备注")
    private String description;

    /**
     * 创建人
     */
    @ApiModelProperty("创建人")
    private String createBy;

    /**
     * 创建时间
     * yyyy-MM-dd HH:mm:ss
     */
    @ApiModelProperty("创建时间")
    private String createTime;

    /**
     * 审核人
     */
    // @ApiModelProperty("审核人")
    // private String approveBy;

    /**
     * 审核时间
     */
    // @ApiModelProperty("审核时间")
    // private String approveTime;

    /**
     * 订单明细
     */
    @ApiModelProperty("订单明细")
    private List<OrderDetailBo> details;

    public PrintSaleOrderBo() {
    }

    @Data
    public static class OrderDetailBo extends BaseBo<SaleOrderFullDto.OrderDetailDto> {
        /**
         * 排序编号
         */
        @ApiModelProperty("排序编号")
        private Integer orderNo;
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
         * SKU编号
         */
        // @ApiModelProperty("SKU编号")
        // private String skuCode;

        /**
         * 简码
         */
        // @ApiModelProperty("简码")
        // private String externalCode;
        /**
         * 规格
         */
        private String spec;
        /**
         * 单位
         */
        private String unit;

        /**
         * 销售数量
         */
        @ApiModelProperty("销售数量")
        private BigDecimal orderNum;

        /**
         * 现价
         */
        @ApiModelProperty("现价")
        private BigDecimal taxPrice;

        /**
         * 销售金额
         */
        @ApiModelProperty("销售金额")
        private BigDecimal orderAmount;
    }
}
