package com.lframework.xingyun.basedata.vo.quote;

import java.math.BigDecimal;
import javax.validation.constraints.*;

import lombok.Data;

/**
 * 报价单商品明细请求。
 */
@Data
public class QuoteSheetProductVo {
    @NotBlank(message = "商品ID不能为空！")
    private String productId;

    @NotNull(message = "销售单价不能为空！")
    @DecimalMin(value = "0", message = "销售单价不能小于0！")
    private BigDecimal salePrice;

    /** 是否询价。 */
    private Boolean inquiryProduct;
}
