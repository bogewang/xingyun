package com.lframework.xingyun.basedata.vo.unit;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUnitVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * ID
   */
  @ApiModelProperty(value = "ID", required = true)
  @NotBlank(message = "ID不能为空！")
  private String id;

  /**
   * 编码
   */
  @ApiModelProperty(value = "编码", required = true)
  @NotBlank(message = "请输入编码！")
  private String code;

  /**
   * 名称
   */
  @ApiModelProperty(value = "名称", required = true)
  @NotBlank(message = "请输入名称！")
  private String name;

  /**
   * 备注
   */
  @ApiModelProperty("备注")
  private String description;
}
