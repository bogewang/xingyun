package com.lframework.xingyun.basedata.vo.customer;

import com.lframework.starter.web.core.vo.BaseVo;
import com.lframework.starter.web.core.vo.PageVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryCustomerSelectorVo extends PageVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 显示标签
   */
  @ApiModelProperty("显示标签")
  private String label;

  /**
   * 编号
   */
  @ApiModelProperty("编号")
  private String code;

  /**
   * 名称
   */
  @ApiModelProperty("名称")
  private String name;

  /**
   * 昵称
   */
  @ApiModelProperty("昵称")
  private String nickName;

  /**
   * 备注
   */
  @ApiModelProperty("备注")
  private String description;

  /**
   * 是否按备注排序
   */
  @ApiModelProperty("是否按备注排序")
  private Boolean orderByDescription;
}
