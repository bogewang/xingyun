package com.lframework.xingyun.basedata.entity.quote;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lframework.starter.web.core.dto.BaseDto;
import com.lframework.starter.web.core.entity.BaseEntity;
import com.lframework.xingyun.basedata.enums.quote.QuoteSheetStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 报价单。
 */
@Data
@TableName("tbl_quote_sheet")
public class QuoteSheet extends BaseEntity implements BaseDto {

  private static final long serialVersionUID = 1L;

  /** ID。 */
  private String id;

  /** 编号。 */
  private String code;

  /** 名称。 */
  private String name;

  /** 生效开始日期。 */
  private LocalDate startDate;

  /** 生效结束日期。 */
  private LocalDate endDate;

  /** 状态。 */
  private QuoteSheetStatus status;

  /** 备注。 */
  private String description;

  /** 租户ID。 */
  private String tenantId;

  /** 创建人ID。 */
  @TableField(fill = FieldFill.INSERT)
  private String createById;

  /** 创建人。 */
  @TableField(fill = FieldFill.INSERT)
  private String createBy;

  /** 创建时间。 */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  /** 修改人。 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private String updateBy;

  /** 修改人ID。 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private String updateById;

  /** 修改时间。 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updateTime;
}
