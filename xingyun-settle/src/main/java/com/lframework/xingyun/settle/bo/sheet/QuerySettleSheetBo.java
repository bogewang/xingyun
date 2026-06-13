package com.lframework.xingyun.settle.bo.sheet;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.collect.Lists;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.utils.DateUtil;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.entity.Supplier;
import com.lframework.xingyun.basedata.service.supplier.SupplierService;
import com.lframework.xingyun.sc.entity.ReceiveSheet;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetService;
import com.lframework.xingyun.settle.entity.SettleSheet;
import com.lframework.starter.web.inner.service.system.SysUserService;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import lombok.Data;

@Data
public class QuerySettleSheetBo extends BaseBo<SettleSheet> {

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
   * 供应商ID
   */
  @ApiModelProperty("供应商ID")
  private String supplierId;

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
   * 总金额
   */
  @ApiModelProperty("总金额")
  private BigDecimal totalAmount;

  /**
   * 优惠金额
   */
  @ApiModelProperty("优惠金额")
  private BigDecimal totalDiscountAmount;

  /**
   * 累计已付
   */
  @ApiModelProperty("累计已付")
  private BigDecimal totalPaidAmt;

  /**
   * 对账总金额
   */
  @ApiModelProperty("对账总金额")
  private BigDecimal totalCheckAmt;

  /**
   * 起始时间
   */
  @ApiModelProperty("起始时间")
  @JsonFormat(pattern = StringPool.DATE_TIME_PATTERN)
  private LocalDateTime startTime;

  /**
   * 截止时间
   */
  @ApiModelProperty("截止时间")
  @JsonFormat(pattern = StringPool.DATE_TIME_PATTERN)
  private LocalDateTime endTime;

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
   * 业务单ID（逗号分隔）
   */
  @ApiModelProperty("业务单ID（逗号分隔）")
  private String bizSheetIds;

  /**
   * 业务单Code（逗号分隔）
   */
  @ApiModelProperty("业务单Code（逗号分隔）")
  private String bizSheetIdCodes;

  public QuerySettleSheetBo() {

  }

  public QuerySettleSheetBo(SettleSheet dto) {

    super(dto);
  }

  @Override
  public BaseBo<SettleSheet> convert(SettleSheet dto) {

    return super.convert(dto, QuerySettleSheetBo::getStatus);
  }

  @Override
  protected void afterInit(SettleSheet dto) {

    SupplierService supplierService = ApplicationUtil.getBean(SupplierService.class);
    Supplier supplier = supplierService.findById(dto.getSupplierId());
    this.supplierCode = supplier.getCode();
    this.supplierName = supplier.getName();

    this.status = dto.getStatus().getCode();

    SysUserService userService = ApplicationUtil.getBean(SysUserService.class);

    if (!StringUtil.isBlank(dto.getApproveBy())) {
      this.approveBy = userService.findById(dto.getApproveBy()).getName();
    }

    this.startTime = dto.getStartDate() != null ? DateUtil.toLocalDateTime(dto.getStartDate()) : null;
    this.endTime = dto.getEndDate() != null ? DateUtil.toLocalDateTimeMax(dto.getEndDate()) : null;

    this.totalPaidAmt = NumberUtil.sub(dto.getTotalCheckAmt() == null ? BigDecimal.ZERO : dto.getTotalCheckAmt(),
            dto.getTotalUnSettleAmt() == null ? BigDecimal.ZERO : dto.getTotalUnSettleAmt());
    this.totalCheckAmt = dto.getTotalCheckAmt();
    this.bizSheetIds = dto.getBizSheetIds();

    ReceiveSheetService receiveSheetService = ApplicationUtil.getBean(ReceiveSheetService.class);
    this.bizSheetIdCodes = receiveSheetService.selectByIds(Lists.newArrayList(dto.getBizSheetIds().split(",")))
            .stream()
            .map(ReceiveSheet::getCode)
            .collect(Collectors.joining(","));
  }
}
