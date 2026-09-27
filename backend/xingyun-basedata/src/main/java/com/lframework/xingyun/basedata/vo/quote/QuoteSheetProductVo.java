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

    /** 本次报价使用的品名，留空时使用共享商品名称。 */
    @Size(max = 128, message = "报价品名长度不能超过128位！")
    private String productName;
  /** 报价规格，允许明确留空。 */
  @Size(max = 128, message = "规格最多128个字符！")
  private String spec;
  /** 商品交易单位ID。 */
  private String unitId;
  /** 交易单位名称快照。 */
  @Size(max = 64, message = "单位名称最多64个字符！")
  private String unitName;
  /** 交易单位换算为基础单位的比例。 */
  @DecimalMin(value = "0", inclusive = false, message = "换算率必须大于0！")
  @Digits(integer = 16, fraction = 8, message = "换算率最多16位整数和8位小数！")
  private BigDecimal conversionRate;


    /** 页面明细排序号。 */
    private Integer orderNo;

    @NotNull(message = "销售单价不能为空！")
    @DecimalMin(value = "0", message = "销售单价不能小于0！")
    private BigDecimal salePrice;

    /** 是否询价。 */
    private Boolean inquiryProduct;
}
