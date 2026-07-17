export interface GetPrintTemplateSettingBo {
  /**
   * ID
   */
  id: string;

  /**
   * 业务类型
   */
  bizType: string;

  /**
   * JSON配置
   */
  templateJson: object;

  /**
   * 示例数据
   */
  demoData: string;

  /**
   * 附加组件配置
   */
  compJsonList: object[];
}
