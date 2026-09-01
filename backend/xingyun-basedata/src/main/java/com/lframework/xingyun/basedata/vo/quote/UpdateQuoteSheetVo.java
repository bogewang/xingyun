package com.lframework.xingyun.basedata.vo.quote;
import javax.validation.constraints.NotBlank; import lombok.Data;
/** 修改报价单请求。 */ @Data public class UpdateQuoteSheetVo extends CreateQuoteSheetVo { @NotBlank(message="ID不能为空！") private String id; }
