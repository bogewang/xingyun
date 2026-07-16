package com.lframework.xingyun.basedata.vo.product.info;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 批量更新商品启用状态请求。
 */
@Data
public class UpdateProductAvailableVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品 ID 列表。
     */
    @ApiModelProperty("商品 ID 列表")
    @NotEmpty(message = "商品 ID 不能为空！")
    private List<String> ids;

    /**
     * 目标启用状态。
     */
    @ApiModelProperty("是否启用")
    @NotNull(message = "启用状态不能为空！")
    private Boolean available;
}
