package com.lframework.xingyun.basedata.vo.print;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePrintTemplateVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * ID
   */
  @ApiModelProperty(value = "ID", required = true)
  @NotNull(message = "ID不能为空！")
  private Integer id;

  /**
   * 名称
   */
  @ApiModelProperty(value = "名称", required = true)
  @NotBlank(message = "请输入名称！")
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
}
