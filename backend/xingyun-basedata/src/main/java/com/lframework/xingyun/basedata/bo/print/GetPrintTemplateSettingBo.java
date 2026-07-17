package com.lframework.xingyun.basedata.bo.print;

import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.xingyun.basedata.entity.PrintTemplate;
import com.lframework.xingyun.basedata.service.print.PrintTemplateCompService;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class GetPrintTemplateSettingBo extends BaseBo<PrintTemplate> {

  /**
   * ID
   */
  @ApiModelProperty("ID")
  private Integer id;

  /**
   * 业务类型
   */
  @ApiModelProperty("业务类型")
  private String bizType;

  /**
   * JSON配置
   */
  @ApiModelProperty("JSON配置")
  private Map<String, Object> templateJson;

  /**
   * 示例数据
   */
  @ApiModelProperty("示例数据")
  private String demoData;

  /**
   * 附加组件配置
   */
  @ApiModelProperty("附加组件配置")
  private List<Map<String, Object>> compJsonList;

  public GetPrintTemplateSettingBo() {

  }

  public GetPrintTemplateSettingBo(PrintTemplate dto) {

    super(dto);
  }

  @Override
  public <A> BaseBo<PrintTemplate> convert(PrintTemplate dto) {
    return super.convert(dto, GetPrintTemplateSettingBo::getTemplateJson,
        GetPrintTemplateSettingBo::getDemoData, GetPrintTemplateSettingBo::getBizType);
  }

  @Override
  protected void afterInit(PrintTemplate dto) {
    this.bizType = dto.getBizType();
    this.templateJson = JsonUtil.parseMap(dto.getTemplateJson(), String.class, Object.class);

    this.demoData = dto.getDemoData();

    PrintTemplateCompService printTemplateCompService = ApplicationUtil.getBean(
        PrintTemplateCompService.class);
    List<String> compJsonList = printTemplateCompService.getCompJsonByTemplateId(dto.getId());
    this.compJsonList = compJsonList.stream()
        .map(t -> JsonUtil.parseMap(t, String.class, Object.class)).collect(Collectors.toList());
  }
}
