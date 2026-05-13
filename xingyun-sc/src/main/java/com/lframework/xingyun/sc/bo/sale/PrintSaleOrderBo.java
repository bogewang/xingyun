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
     * 客户名称
     */
    @ApiModelProperty("客户名称")
    private String customerName;

    /**
     * 备注
     */
    @ApiModelProperty("备注")
    private String description;

    /**
     * 已付金额
     */
    @ApiModelProperty("已付金额")
    private BigDecimal paidAmount;

    /**
     * 未付金额
     */
    @ApiModelProperty("未付金额")
    private BigDecimal unpaidAmount;

    /**
     * 创建人
     */
    @ApiModelProperty("创建人")
    private String createBy;

    /**
     * 送货日期
     * yyyy-MM-dd HH:mm:ss
     */
    @ApiModelProperty("送货日期")
    private String deliveryDate;

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
        private Integer seq;
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
