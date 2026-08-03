package com.lframework.xingyun.basedata.bo.print;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.xingyun.basedata.entity.PrintTemplate;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QueryPrintTemplateBo extends BaseBo<PrintTemplate> {

  /**
   * ID
   */
  @ApiModelProperty("ID")
  private Integer id;

  /**
   * 名称
   */
  @ApiModelProperty("名称")
  private String name;

  /**
   * 语言
   */
  @ApiModelProperty("语言")
  private String lang;

  /**
   * 业务类型
   */
  @ApiModelProperty("业务类型")
  private String bizType;

  /**
   * 版本
   */
  @ApiModelProperty("版本")
  private String version;

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
   * 修改人
   */
  @ApiModelProperty("修改人")
  private String updateBy;

  /**
   * 修改时间
   */
  @ApiModelProperty("修改时间")
  @JsonFormat(pattern = StringPool.DATE_TIME_PATTERN)
  private LocalDateTime updateTime;

  /**
   * 是否默认模板
   */
  @ApiModelProperty("是否默认模板")
  private Boolean isDefault;

  public QueryPrintTemplateBo() {

  }

  public QueryPrintTemplateBo(PrintTemplate dto) {

    super(dto);
  }
}
