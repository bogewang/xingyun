package com.lframework.xingyun.sc.vo.purchase;

import com.lframework.starter.web.core.vo.PageVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class QueryPurchaseProductVo extends PageVo {

  private static final long serialVersionUID = 1L;

  /**
   * 仓库ID
   */
  @ApiModelProperty("仓库ID")
  private String scId;

  /**
   * 检索关键字
   */
  @ApiModelProperty("检索关键字")
  private String condition;

  /**
   * 分类ID
   */
  @ApiModelProperty("分类ID")
  private String categoryId;

  /**
   * 品牌ID
   */
  @ApiModelProperty("品牌ID")
  private String brandId;

  /**
   * 是否退货
   */
  @ApiModelProperty("是否退货")
  private Boolean isReturn = Boolean.FALSE;
}
