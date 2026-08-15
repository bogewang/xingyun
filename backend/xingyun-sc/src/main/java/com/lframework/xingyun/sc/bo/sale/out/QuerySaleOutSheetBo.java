package com.lframework.xingyun.sc.bo.sale.out;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.inner.service.system.SysUserService;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.sc.entity.SaleOrder;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.service.sale.SaleOrderService;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class QuerySaleOutSheetBo extends BaseBo<SaleOutSheet> {

    /**
   * ID
   */
    @ApiModelProperty("ID")
    private String id;

    /**
   * 单号
   */
    @ApiModelProperty("单号")
    private String code;

    /**
   * 仓库编号
   */
    @ApiModelProperty("仓库编号")
    private String scCode;

    /**
   * 仓库名称
   */
    @ApiModelProperty("仓库名称")
    private String scName;

    /**
     * 客户编号
     */
    @ApiModelProperty("客户编号")
    private String customerCode;

    /**
     * 客户名称
     */
    @ApiModelProperty("客户名称")
    private String customerName;

    /**
     * 客户备注
     */
    @ApiModelProperty("客户备注")
    private String customerDescription;

    /**
     * 销售员姓名
     */
    @ApiModelProperty("销售员姓名")
    private String salerName;

    /**
     * 订单日期
     */
    @ApiModelProperty("订单日期")
    @JsonFormat(pattern = StringPool.DATE_PATTERN)
    private LocalDate orderDate;

    /**
     * 销售订单ID
     */
    @ApiModelProperty("销售订单ID")
    private String saleOrderId;

    /**
     * 销售订单号
     */
    @ApiModelProperty("销售订单号")
    private String saleOrderCode;

    /**
     * 销售数量
     */
    @ApiModelProperty("销售数量")
    private BigDecimal totalNum;

    /**
     * 赠品数量
     */
    @ApiModelProperty("赠品数量")
    private BigDecimal totalGiftNum;

    /**
     * 销售金额
     */
    @ApiModelProperty("销售金额")
    private BigDecimal totalAmount;

    /**
     * 成本单价
     */
    @ApiModelProperty("成本单价")
    private BigDecimal totalCost;

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
     * 总利润
     */
    @ApiModelProperty("总利润")
    private BigDecimal totalProfit;

    /**
     * 是否录完所有成本
     */
    @ApiModelProperty("是否录完所有成本")
    private Boolean fillAllCost;

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
     */
    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = StringPool.DATE_TIME_PATTERN)
    private LocalDateTime createTime;

    /**
     * 审核人
     */
    @ApiModelProperty("审核人")
    private String approveBy;

    /**
     * 审核时间
     */
    @ApiModelProperty("审核时间")
    @JsonFormat(pattern = StringPool.DATE_TIME_PATTERN)
    private LocalDateTime approveTime;

    /**
     * 状态
     */
    @ApiModelProperty("状态")
    private Integer status;

    /**
     * 拒绝原因
     */
    @ApiModelProperty("拒绝原因")
    private String refuseReason;

    /**
     * 结算状态
     */
    @ApiModelProperty("结算状态")
    private Integer settleStatus;

    /**
     * 验收金额
     */
    @ApiModelProperty("验收金额")
    private BigDecimal confirmAmt;

    /**
     * 验收数量
     */
    @ApiModelProperty("验收数量")
    private BigDecimal confirmNum;

    public QuerySaleOutSheetBo(SaleOutSheet dto) {

        super(dto);
    }

    @Override
    public BaseBo<SaleOutSheet> convert(SaleOutSheet dto) {

        return super.convert(dto, QuerySaleOutSheetBo::getStatus, QuerySaleOutSheetBo::getSettleStatus);
    }

    @Override
    protected void afterInit(SaleOutSheet dto) {
        CustomerService customerService = ApplicationUtil.getBean(CustomerService.class);
        Customer customer = customerService.findById(dto.getCustomerId());
        this.customerCode = customer.getCode();
        this.customerName = customer.getName();
        this.customerDescription = customer.getDescription();

        SysUserService userService = ApplicationUtil.getBean(SysUserService.class);
        if (!StringUtil.isBlank(dto.getSalerId())) {
            this.salerName = userService.findById(dto.getSalerId()).getName();
        }

        if (!StringUtil.isBlank(dto.getApproveBy())) {
            this.approveBy = userService.findById(dto.getApproveBy()).getName();
        }

        this.status = dto.getStatus().getCode();
        this.settleStatus = dto.getSettleStatus().getCode();
        this.paidAmount = dto.getPaidAmount() == null ? BigDecimal.ZERO : dto.getPaidAmount();
        this.unpaidAmount = calculateUnpaidAmount(dto.getTotalAmount(), dto.getConfirmAmt(), this.paidAmount);

        if (!StringUtil.isBlank(dto.getSaleOrderId())) {
            SaleOrderService saleOrderService = ApplicationUtil.getBean(SaleOrderService.class);
            SaleOrder saleOrder = saleOrderService.getById(dto.getSaleOrderId());
            this.saleOrderCode = saleOrder.getCode();
        }
        this.confirmNum = dto.getConfirmNum();
        this.confirmAmt = dto.getConfirmAmt();
    }

    /** 验收金额非零时，未付金额按验收金额减已付金额计算。 */
    static BigDecimal calculateUnpaidAmount(BigDecimal totalAmount, BigDecimal confirmAmt,
            BigDecimal paidAmount) {
        BigDecimal actualConfirmAmt = confirmAmt == null ? BigDecimal.ZERO : confirmAmt;
        BigDecimal receivableAmount = actualConfirmAmt.compareTo(BigDecimal.ZERO) == 0
                ? (totalAmount == null ? BigDecimal.ZERO : totalAmount)
                : actualConfirmAmt;
        return NumberUtil.sub(receivableAmount, paidAmount == null ? BigDecimal.ZERO : paidAmount);
    }
}
