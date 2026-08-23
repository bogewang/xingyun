export interface QuerySupplierBo {
  /**
   * ID
   */
  id: string;

  /**
   * 编号
   */
  code: string;

  /**
   * 名称
   */
  name: string;

  /**
   * 状态；true 表示启用，false 表示停用
   */
  available: boolean;

  /**
   * 备注
   */
  description: string;

  /**
   * 已付金额
   */
  paidAmount: number;

  /**
   * 未付金额
   */
  unpaidAmount: number;

  /**
   * 创建人ID
   */
  createBy: string;

  /**
   * 创建时间
   */
  createTime: string;

  /**
   * 修改人ID
   */
  updateBy: string;

  /**
   * 修改时间
   */
  updateTime: string;
}
