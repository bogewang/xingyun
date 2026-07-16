package com.lframework.xingyun.basedata.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lframework.starter.web.core.dto.BaseDto;
import com.lframework.starter.web.core.entity.BaseEntity;
import lombok.Data;
import java.time.LocalDateTime;

/** 可复用的计量单位字典。商品单位换算率仍按商品单独配置。 */
@Data
@TableName("base_data_unit")
public class Unit extends BaseEntity implements BaseDto {
  private String id;
  private String code;
  private String name;
  private Boolean available;
  private String description;

  @TableField(fill = FieldFill.INSERT)
  private String createById;
  @TableField(fill = FieldFill.INSERT)
  private String createBy;
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private String updateBy;
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private String updateById;
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updateTime;
}
