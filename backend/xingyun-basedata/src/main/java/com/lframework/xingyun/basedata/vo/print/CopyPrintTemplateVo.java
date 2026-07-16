package com.lframework.xingyun.basedata.vo.print;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CopyPrintTemplateVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 来源模板ID
   */
  @ApiModelProperty(value = "来源模板ID", required = true)
  @NotNull(message = "来源模板ID不能为空！")
  private Integer sourceId;

  /**
   * 名称
   */
  @ApiModelProperty(value = "名称", required = true)
  @NotBlank(message = "请输入名称！")
  private String name;

  /**
   * 业务类型
   */
  @ApiModelProperty(value = "业务类型", required = true)
  @NotBlank(message = "请选择业务类型！")
  private String bizType;
}
