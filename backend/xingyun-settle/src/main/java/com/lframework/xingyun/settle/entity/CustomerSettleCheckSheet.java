package com.lframework.xingyun.settle.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lframework.starter.web.core.dto.BaseDto;
import com.lframework.starter.web.core.entity.BaseEntity;
import com.lframework.xingyun.settle.enums.CustomerSettleCheckSheetStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 客户对账单。
 */
@Data
@TableName("customer_settle_check_sheet")
public class CustomerSettleCheckSheet extends BaseEntity implements BaseDto {

  private static final long serialVersionUID = 1L;

  /** 对账单ID。 */
  private String id;

  /** 对账单编号。 */
  private String code;

  /** 客户ID。 */
  private String customerId;

  /** 业务单据原始金额合计。 */
  private BigDecimal totalAmount;

  /** 确认对账金额。 */
  private BigDecimal totalPayAmount;

  /** 已结算金额。 */
  private BigDecimal totalPayedAmount;

  /**
   *
   */
  private BigDecimal totalDiscountAmount;

  /** 备注。 */
  private String description;

  /** 创建人ID。 */
  @TableField(fill = FieldFill.INSERT)
  private String createById;

  /** 创建人。 */
  @TableField(fill = FieldFill.INSERT)
  private String createBy;

  /** 创建时间。 */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  /** 修改人ID。 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private String updateById;

  /** 修改人。 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private String updateBy;

  /** 修改时间。 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updateTime;

  /** 确认人。 */
  private String approveBy;

  /** 确认时间。 */
  private LocalDateTime approveTime;

  /** 对账单状态。 */
  private CustomerSettleCheckSheetStatus status;
}
