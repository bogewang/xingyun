package com.lframework.xingyun.settle.entity;

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

  /** 备注。 */
  private String description;

  /** 确认人。 */
  private String approveBy;

  /** 确认时间。 */
  private LocalDateTime approveTime;

  /** 对账单状态。 */
  private CustomerSettleCheckSheetStatus status;
}
