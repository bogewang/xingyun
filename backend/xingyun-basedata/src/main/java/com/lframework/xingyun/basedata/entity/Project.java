package com.lframework.xingyun.basedata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lframework.starter.web.core.entity.BaseEntity;
import lombok.Data;

/** 租户内项目。 */
@Data
@TableName("sys_project")
public class Project extends BaseEntity {
  /** 项目ID。 */
  private String id;
  /** 项目编码。 */
  private String code;
  /** 项目名称。 */
  private String name;
  /** 是否启用。 */
  private Boolean available;
}
