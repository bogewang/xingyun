package com.lframework.xingyun.sc.vo.purchase.receive;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.exceptions.impl.InputErrorException;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.vo.BaseVo;
import com.lframework.xingyun.basedata.service.storecenter.StoreCenterService;
import com.lframework.xingyun.sc.entity.PurchaseConfig;
import com.lframework.xingyun.sc.service.purchase.PurchaseConfigService;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class CreateReceiveSheetVo implements BaseVo, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 仓库ID
     */
    @ApiModelProperty("仓库ID")
    private String scId;

    /**
     * 供应商ID
     */
    @ApiModelProperty(value = "供应商ID", required = true)
    @NotBlank(message = "供应商ID不能为空！")
    private String supplierId;

    /**
     * 采购员ID
     */
    @ApiModelProperty("采购员ID")
    private String purchaserId;

    /**
     * 订单日期
     */
    @ApiModelProperty("订单日期")
    @NotNull(message = "订单日期不能为空！")
    private LocalDate orderDate;

    /**
     * 付款日期
     */
    @ApiModelProperty("付款日期")
    private LocalDate paymentDate;

    /**
     * 折后金额
     */
    @ApiModelProperty("折后金额")
    private BigDecimal totalAmount;

    /**
     * 付款金额
     */
    @ApiModelProperty("付款金额")
    private BigDecimal paidAmount;

    /**
     * 是否允许修改付款日期
     */
    @ApiModelProperty("是否允许修改付款日期")
    private Boolean allowModifyPaymentDate = Boolean.FALSE;

    /**
     * 到货日期
     */
    @ApiModelProperty(value = "到货日期", required = true)
    private LocalDate receiveDate;

    /**
     * 采购订单ID
     */
    @ApiModelProperty("采购订单ID")
    private String purchaseOrderId;

    /**
     * 商品信息
     */
    @ApiModelProperty(value = "商品信息", required = true)
    @Valid
    @NotEmpty(message = "商品不能为空！")
    private List<ReceiveProductVo> products;

    /**
     * 备注
     */
    @ApiModelProperty("备注")
    private String description;

    /**
     * 是否关联采购订单
     */
    @ApiModelProperty("是否关联采购订单")
    private Boolean required;

    public void validate() {

        PurchaseConfigService purchaseConfigService = ApplicationUtil.getBean(
                PurchaseConfigService.class);
        PurchaseConfig purchaseConfig = purchaseConfigService.get();

        if (!purchaseConfig.getReceiveRequirePurchase().equals(this.required)) {
            throw new DefaultClientException("系统参数发生改变，请刷新页面后重试！");
        }

        if (StringUtils.isBlank(this.scId)) {
            StoreCenterService storeCenterService = ApplicationUtil.getBean(StoreCenterService.class);
            this.scId = storeCenterService.getDefaultStoreId();
        }

        this.validate(purchaseConfig.getReceiveRequirePurchase());
    }

    protected void validate(boolean requirePurchase) {
        this.products = this.products.stream()
                .filter(product -> product != null && StringUtil.isNotBlank(product.getProductId()))
                .collect(Collectors.toList());
        if (this.products.isEmpty()) {
            throw new InputErrorException("请录入商品！");
        }

        int orderNo = 1;
        if (this.totalAmount != null) {
            if (NumberUtil.lt(this.totalAmount, BigDecimal.ZERO)) {
                throw new InputErrorException("折后金额不允许小于0！");
            }

            if (!NumberUtil.isNumberPrecision(this.totalAmount, 2)) {
                throw new InputErrorException("折后金额最多允许2位小数！");
            }
        }

        if (this.paidAmount != null) {
            if (NumberUtil.lt(this.paidAmount, BigDecimal.ZERO)) {
                throw new InputErrorException("付款金额不允许小于0！");
            }

            if (!NumberUtil.isNumberPrecision(this.paidAmount, 6)) {
                throw new InputErrorException("付款金额最多允许6位小数！");
            }
        }

        for (ReceiveProductVo product : this.products) {

            if (StringUtil.isBlank(product.getProductId())) {
                throw new InputErrorException("第" + orderNo + "行商品不能为空！");
            }

            if (product.getReceiveNum() != null) {
                if (NumberUtil.lt(product.getReceiveNum(), BigDecimal.ZERO)) {
                    throw new InputErrorException("第" + orderNo + "行商品收货数量不允许小于0！");
                }

                if (!NumberUtil.isNumberPrecision(product.getReceiveNum(), 8)) {
                    throw new InputErrorException("第" + orderNo + "行商品收货数量最多允许8位小数！");
                }
            }

            if (product.getPurchasePrice() != null) {
                if (NumberUtil.lt(product.getPurchasePrice(), BigDecimal.ZERO)) {
                    throw new InputErrorException("第" + orderNo + "行商品采购价不允许小于0！");
                }

                if (!NumberUtil.isNumberPrecision(product.getPurchasePrice(), 6)) {
                    throw new InputErrorException("第" + orderNo + "行商品采购价最多允许6位小数！");
                }
            }
            product.setSeq(orderNo);

            orderNo++;
        }
    }
}
