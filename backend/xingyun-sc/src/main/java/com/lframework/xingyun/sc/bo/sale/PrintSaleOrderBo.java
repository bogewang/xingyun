package com.lframework.xingyun.sc.bo.sale;

import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.xingyun.sc.dto.sale.SaleOrderFullDto;
import com.lframework.xingyun.sc.enums.SaleOutSheetStatus;
import com.lframework.xingyun.sc.enums.SettleStatus;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
     * 客户简称
     */
    @ApiModelProperty("客户简称")
    private String customerNickName;

    /**
     * 客户备注
     */
    @ApiModelProperty("客户备注")
    private String customerDescription;

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
    @ApiModelProperty("销售日期")
    private String orderDate;

    /**
     * ID
     */
    @ApiModelProperty("ID")
    private String id;

    /**
     * 仓库ID
     */
    @ApiModelProperty("仓库ID")
    private String scId;

    /**
     * 客户ID
     */
    @ApiModelProperty("客户ID")
    private String customerId;

    /**
     * 销售员ID
     */
    @ApiModelProperty("销售员ID")
    private String salerId;

    /**
     * 付款日期
     */
    @ApiModelProperty("付款日期")
    private LocalDate paymentDate;

    /**
     * 商品数量
     */
    @ApiModelProperty("商品数量")
    private BigDecimal totalNum;

    /**
     * 赠品数量
     */
    @ApiModelProperty("赠品数量")
    private BigDecimal totalGiftNum;

    /**
     * 出库总金额
     */
    @ApiModelProperty("出库总金额")
    private BigDecimal totalAmount;

    /**
     * 总成本
     */
    @ApiModelProperty("总成本")
    private BigDecimal totalCost;

    @ApiModelProperty("总利润")
    private BigDecimal totalProfit;

    /**
     * 是否录完所有成本
     */
    @ApiModelProperty("是否录完所有成本")
    private Boolean fillAllCost;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    /**
     * 修改人
     */
    @ApiModelProperty("修改人")
    private String updateBy;

    /**
     * 修改时间
     */
    @ApiModelProperty("修改时间")
    private LocalDateTime updateTime;

    /**
     * 审核人
     */
    @ApiModelProperty("审核人")
    private String approveBy;

    /**
     * 审核时间
     */
    @ApiModelProperty("审核时间")
    private LocalDateTime approveTime;

    /**
     * 状态
     */
    @ApiModelProperty("状态")
    private SaleOutSheetStatus status;

    /**
     * 拒绝原因
     */
    @ApiModelProperty("拒绝原因")
    private String refuseReason;

    /**
     * 销售订单ID
     */
    @ApiModelProperty("销售订单ID")
    private String saleOrderId;

    /**
     * 结算状态
     */
    @ApiModelProperty("结算状态")
    private SettleStatus settleStatus;

    @ApiModelProperty("验收数量")
    private BigDecimal confirmNum;

    @ApiModelProperty("验收金额")
    private BigDecimal confirmAmt;

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
        @ApiModelProperty("规格")
        private String spec;
        /**
         * 单位
         */
        @ApiModelProperty("单位")
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

        /**
         * 明细ID
         */
        @ApiModelProperty("明细ID")
        private String id;

        /**
         * 组合商品ID
         */
        @ApiModelProperty("组合商品ID")
        private String mainProductId;

        /**
         * 商品ID
         */
        @ApiModelProperty("商品ID")
        private String productId;

        /**
         * 原价
         */
        @ApiModelProperty("原价")
        private BigDecimal oriPrice;



        @ApiModelProperty("折扣（%）")
        private BigDecimal discountRate;

        /**
         * 是否赠品
         */
        @ApiModelProperty("是否赠品")
        private Boolean isGift;

        /**
         * 税率（%）
         */
        @ApiModelProperty("税率（%）")
        private BigDecimal taxRate;

        @ApiModelProperty("备注")
        private String description;

        /**
         * 排序编号
         */
        @ApiModelProperty("排序编号")
        private Integer orderNo;

        /**
         * 结算状态
         */
        @ApiModelProperty("结算状态")
        private SettleStatus settleStatus;

        /**
         * 销售订单明细ID
         */
        @ApiModelProperty("销售订单明细ID")
        private String saleOrderDetailId;

        /**
         * 总金额
         */
        @ApiModelProperty("总金额")
        private BigDecimal taxAmount;

        /**
         * 成本单价
         */
        @ApiModelProperty("成本单价")
        private BigDecimal costPrice;

        /**
         * 是否手动录入成本
         */
        @ApiModelProperty("是否手动录入成本")
        private Boolean manualInputCost;

        /**
         * 总利润
         */
        @ApiModelProperty("利润")
        private BigDecimal totalProfit;

        /**
         * 商品分类名称
         */
        @ApiModelProperty("商品分类名称")
        private String categoryName;

        @ApiModelProperty("验收数量")
        private BigDecimal confirmNum;

        @ApiModelProperty("验收金额")
        private BigDecimal confirmAmt;
    }
}
