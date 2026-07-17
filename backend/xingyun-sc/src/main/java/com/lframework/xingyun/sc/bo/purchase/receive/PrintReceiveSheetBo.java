package com.lframework.xingyun.sc.bo.purchase.receive;

import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.DateUtil;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.entity.StoreCenter;
import com.lframework.xingyun.basedata.entity.Supplier;
import com.lframework.xingyun.basedata.service.storecenter.StoreCenterService;
import com.lframework.xingyun.basedata.service.supplier.SupplierService;
import com.lframework.xingyun.sc.dto.purchase.PurchaseProductDto;
import com.lframework.xingyun.sc.dto.purchase.receive.ReceiveSheetFullDto;
import com.lframework.xingyun.sc.entity.PurchaseOrder;
import com.lframework.xingyun.sc.enums.ReceiveSheetStatus;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.service.purchase.PurchaseOrderService;
import com.lframework.starter.web.inner.service.system.SysUserService;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Data;

@Data
public class PrintReceiveSheetBo extends BaseBo<ReceiveSheetFullDto> {

  /**
   * 单号
   */
  @ApiModelProperty("单号")
  private String code;

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
   * 供应商编号
   */
  @ApiModelProperty("供应商编号")
  private String supplierCode;

  /**
   * 供应商名称
   */
  @ApiModelProperty("供应商名称")
  private String supplierName;

  /**
   * 供应商ID
   */
  @ApiModelProperty("供应商ID")
  private String supplierId;

  /**
   * 采购员姓名
   */
  @ApiModelProperty("采购员姓名")
  private String purchaserName;

  /**
   * 采购员ID
   */
  @ApiModelProperty("采购员ID")
  private String purchaserId;

  /**
   * 订单日期
   */
  @ApiModelProperty("订单日期")
  private String orderDate;

  /**
   * 付款日期
   */
  @ApiModelProperty("付款日期")
  private String paymentDate;

  /**
   * 到货日期
   */
  @ApiModelProperty("到货日期")
  private String receiveDate;

  /**
   * 采购订单ID
   */
  @ApiModelProperty("采购订单ID")
  private String purchaseOrderId;

  /**
   * 采购订单号
   */
  @ApiModelProperty("采购订单号")
  private String purchaseOrderCode;

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
   * 采购数量
   */
  @ApiModelProperty("采购数量")
  private BigDecimal totalNum;

  /**
   * 赠品数量
   */
  @ApiModelProperty("赠品数量")
  private BigDecimal totalGiftNum;

  /**
   * 采购金额
   */
  @ApiModelProperty("采购金额")
  private BigDecimal totalAmount;

  /**
   * 创建人
   */
  @ApiModelProperty("创建人")
  private String createBy;

  /**
   * 创建时间
   */
  @ApiModelProperty("创建时间")
  private String createTime;

  /**
   * 修改人
   */
  @ApiModelProperty("修改人")
  private String updateBy;

  /**
   * 修改时间
   */
  @ApiModelProperty("修改时间")
  private String updateTime;

  /**
   * 审核人
   */
  @ApiModelProperty("审核人")
  private String approveBy;

  /**
   * 审核时间
   */
  @ApiModelProperty("审核时间")
  private String approveTime;

  /**
   * 状态
   */
  @ApiModelProperty("状态")
  private ReceiveSheetStatus status;

  /**
   * 拒绝原因
   */
  @ApiModelProperty("拒绝原因")
  private String refuseReason;

  /**
   * 结算状态
   */
  @ApiModelProperty("结算状态")
  private SettleStatus settleStatus;

  /**
   * 订单明细
   */
  @ApiModelProperty("订单明细")
  private List<OrderDetailBo> details;

  public PrintReceiveSheetBo() {

  }

  public PrintReceiveSheetBo(ReceiveSheetFullDto dto) {

    super(dto);
  }

  @Override
  public BaseBo<ReceiveSheetFullDto> convert(ReceiveSheetFullDto dto) {

    return super.convert(dto, PrintReceiveSheetBo::getDetails);
  }

  @Override
  protected void afterInit(ReceiveSheetFullDto dto) {

    this.purchaserName = StringPool.EMPTY_STR;
    this.orderDate = StringPool.EMPTY_STR;
    this.paymentDate = StringPool.EMPTY_STR;
    this.receiveDate = StringPool.EMPTY_STR;
    this.purchaseOrderCode = StringPool.EMPTY_STR;
    this.approveBy = StringPool.EMPTY_STR;
    this.approveTime = StringPool.EMPTY_STR;

    StoreCenterService storeCenterService = ApplicationUtil.getBean(StoreCenterService.class);
    StoreCenter sc = storeCenterService.findById(dto.getScId());
    this.scCode = sc.getCode();
    this.scName = sc.getName();

    SupplierService supplierService = ApplicationUtil.getBean(SupplierService.class);
    Supplier supplier = supplierService.findById(dto.getSupplierId());
    this.supplierCode = supplier.getCode();
    this.supplierName = supplier.getName();

    SysUserService userService = ApplicationUtil.getBean(SysUserService.class);
    if (!StringUtil.isBlank(dto.getPurchaserId())) {
      this.purchaserName = userService.findById(dto.getPurchaserId()).getName();
    }

    PurchaseOrderService purchaseOrderService = ApplicationUtil.getBean(PurchaseOrderService.class);
    if (!StringUtil.isBlank(dto.getPurchaseOrderId())) {
      PurchaseOrder purchaseOrder = purchaseOrderService.getById(dto.getPurchaseOrderId());
      this.purchaseOrderCode = purchaseOrder.getCode();
    }

    if (dto.getOrderDate() != null) {
      this.orderDate = DateUtil.formatDate(dto.getOrderDate());
    }

    if (dto.getPaymentDate() != null) {
      this.paymentDate = DateUtil.formatDate(dto.getPaymentDate());
    }

    if (dto.getReceiveDate() != null) {
      this.receiveDate = DateUtil.formatDate(dto.getReceiveDate());
    }

    this.paidAmount = dto.getPaidAmount() == null ? BigDecimal.ZERO : dto.getPaidAmount();
    this.unpaidAmount = NumberUtil.sub(dto.getTotalAmount(), this.paidAmount);
    this.createTime = DateUtil.formatDateTime(dto.getCreateTime());

    if (!StringUtil.isBlank(dto.getApproveBy())
        && dto.getStatus() == ReceiveSheetStatus.APPROVE_PASS) {
      this.approveBy = userService.findById(dto.getApproveBy()).getName();
      this.approveTime = DateUtil.formatDateTime(dto.getApproveTime());
    }

    if (!CollectionUtil.isEmpty(dto.getDetails())) {
      this.details = IntStream.range(0, dto.getDetails().size())
          .mapToObj(index -> {
            OrderDetailBo detail = new OrderDetailBo(dto.getDetails().get(index));
            detail.setSeq(index + 1);
            return detail;
          }).collect(Collectors.toList());
    }
  }

  @Data
  public static class OrderDetailBo extends BaseBo<ReceiveSheetFullDto.OrderDetailDto> {

    /**
     * 明细ID
     */
    @ApiModelProperty("明细ID")
    private String id;

    /**
     * 商品ID
     */
    @ApiModelProperty("商品ID")
    private String productId;

    /**
     * 收货数量原始值
     */
    @ApiModelProperty("收货数量原始值")
    private BigDecimal orderNum;

    /**
     * 单位ID
     */
    @ApiModelProperty("单位ID")
    private String unitId;

    /**
     * 单位名称
     */
    @ApiModelProperty("单位名称")
    private String unitName;

    /**
     * 换算率
     */
    @ApiModelProperty("换算率")
    private BigDecimal conversionRate;

    /**
     * 业务数量
     */
    @ApiModelProperty("业务数量")
    private BigDecimal businessNum;

    /**
     * 采购价原始值
     */
    @ApiModelProperty("采购价原始值")
    private BigDecimal taxPrice;

    /**
     * 采购总金额原始值
     */
    @ApiModelProperty("采购总金额原始值")
    private BigDecimal taxAmount;

    /**
     * 是否赠品
     */
    @ApiModelProperty("是否赠品")
    private Boolean isGift;

    /**
     * 税率
     */
    @ApiModelProperty("税率")
    private BigDecimal taxRate;

    /**
     * 明细备注
     */
    @ApiModelProperty("明细备注")
    private String description;

    /**
     * 排序编号
     */
    @ApiModelProperty("排序编号")
    private Integer orderNo;

    /**
     * 采购订单明细ID
     */
    @ApiModelProperty("采购订单明细ID")
    private String purchaseOrderDetailId;

    /**
     * 生产日期
     */
    @ApiModelProperty("生产日期")
    private String productionDate;

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
    @ApiModelProperty("SKU编号")
    private String skuCode;

    /**
     * 简码
     */
    @ApiModelProperty("简码")
    private String externalCode;

    /**
     * 收货数量
     */
    @ApiModelProperty("收货数量")
    private BigDecimal receiveNum;

    /**
     * 采购价
     */
    @ApiModelProperty("采购价")
    private BigDecimal purchasePrice;

    /**
     * 收货金额
     */
    @ApiModelProperty("收货金额")
    private BigDecimal receiveAmount;

    /**
     * 规格型号
     */
    @ApiModelProperty("规格型号")
    private String spec;
    /**
     * 序号
     */
    @ApiModelProperty("序号")
    private Integer seq;

    public OrderDetailBo(ReceiveSheetFullDto.OrderDetailDto dto) {

      this.init(dto);
    }

    @Override
    public BaseBo<ReceiveSheetFullDto.OrderDetailDto> convert(
        ReceiveSheetFullDto.OrderDetailDto dto) {

      return super.convert(dto);
    }

    @Override
    protected void afterInit(ReceiveSheetFullDto.OrderDetailDto dto) {

      this.receiveNum = dto.getOrderNum();
      this.purchasePrice = dto.getTaxPrice();
      this.receiveAmount = dto.getTaxAmount();

      PurchaseOrderService purchaseOrderService = ApplicationUtil.getBean(
          PurchaseOrderService.class);
      PurchaseProductDto product = purchaseOrderService.getPurchaseById(dto.getProductId());

      this.productCode = product.getCode();
      this.productName = product.getName();
      this.skuCode = product.getSkuCode();
      this.externalCode = product.getExternalCode();
    }
  }
}
