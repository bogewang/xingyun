package com.lframework.xingyun.basedata.vo.quote;
import io.swagger.annotations.ApiModelProperty; import java.time.LocalDate; import java.util.List; import javax.validation.Valid; import javax.validation.constraints.*; import lombok.Data;
/** 新增报价单请求。 */ @Data public class CreateQuoteSheetVo {
 @ApiModelProperty(required=true) @NotBlank(message="编号不能为空！") private String code; @ApiModelProperty(required=true) @NotBlank(message="名称不能为空！") private String name;
 @NotNull(message="生效开始日期不能为空！") private LocalDate startDate; @NotNull(message="生效结束日期不能为空！") private LocalDate endDate; private String description;
 @Valid @NotEmpty(message="商品明细不能为空！") private List<QuoteSheetProductVo> products;
}
