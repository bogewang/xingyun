export interface UpdatePrintTemplateSettingVo {
  /**
   * ID
   */
  id: string;

  /**
   * JSON配置
   */
  templateJson: string;

  /**
   * 示例数据
   */
  demoData?: string;
}
