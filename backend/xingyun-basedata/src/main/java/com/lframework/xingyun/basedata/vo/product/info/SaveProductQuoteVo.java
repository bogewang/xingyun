package com.lframework.xingyun.basedata.vo.product.info;

import java.math.BigDecimal;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Data;

/** 保存商品在多个报价单中的价格和询价标识。 */
@Data
public class SaveProductQuoteVo {
    @NotBlank(message = "商品ID不能为空！")
    private String productId;
    @Valid
    @NotEmpty(message = "请选择报价单！")
    private List<@NotNull QuoteRow> quotes;

    /** 单个报价单的编辑数据。 */
    @Data
    public static class QuoteRow {
        @NotBlank(message = "报价单ID不能为空！")
        private String quoteSheetId;
        @NotNull(message = "价格不能为空！")
        @DecimalMin(value = "0", message = "价格不能小于0！")
        @Digits(integer = 16, fraction = 2, message = "价格最多支持16位整数和2位小数！")
        private BigDecimal salePrice;
        @NotNull(message = "请选择是否询价！")
        private Boolean inquiryProduct;
    }
}
