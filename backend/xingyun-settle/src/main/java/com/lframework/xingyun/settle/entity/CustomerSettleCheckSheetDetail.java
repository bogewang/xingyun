package com.lframework.xingyun.settle.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lframework.starter.web.core.dto.BaseDto;
import com.lframework.starter.web.core.entity.BaseEntity;
import com.lframework.xingyun.settle.enums.CustomerSettleCheckSheetBizType;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 客户对账单明细。
 */
@Data
@TableName("customer_settle_check_sheet_detail")
public class CustomerSettleCheckSheetDetail extends BaseEntity implements BaseDto {

  private static final long serialVersionUID = 1L;

  /** 明细ID。 */
  private String id;

  /** 对账单ID。 */
  private String sheetId;

  /** 业务单据ID。 */
  private String bizId;

  /** 业务类型。 */
  private CustomerSettleCheckSheetBizType bizType;

  /** 最终对账金额。 */
  private BigDecimal payAmount;

  /** 备注。 */
  private String description;

  /** 排序号。 */
  private Integer orderNo;
}
