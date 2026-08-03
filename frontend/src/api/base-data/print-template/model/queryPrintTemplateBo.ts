export interface QueryPrintTemplateBo {
  /**
   * ID
   */
  id: string;

  /**
   * 名称
   */
  name: string;

  /**
   * 语言
   */
  lang: string;

  /**
   * 业务类型
   */
  bizType: string;

  /**
   * 版本
   */
  version: string;

  /**
   * 创建人
   */
  createBy: string;

  /**
   * 创建时间
   */
  createTime: string;

  /**
   * 修改人
   */
  updateBy: string;

  /**
   * 修改时间
   */
  updateTime: string;

  /**
   * 是否默认模板
   */
  isDefault?: boolean;
}
