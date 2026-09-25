package com.lframework.xingyun.basedata.vo.product.info;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import lombok.Data;
/** 商品追加报价单请求。 */
@Data
public class AddProductQuoteVo {
    @NotBlank(message = "商品ID不能为空！")
    private String productId;
    @NotEmpty(message = "请选择报价单！")
    private List<@NotBlank(message = "报价单ID不能为空！") String> quoteSheetIds;
}
