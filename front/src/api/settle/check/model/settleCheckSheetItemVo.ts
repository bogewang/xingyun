export interface SettleCheckSheetItemVo {
  /**
   * 单据ID
   */
  id: string;

  /**
   * 业务类型
   */
  bizType: number;

  /**
   * 货流金额
   */
  bizAmount?: number;

  /**
   * 备注
   */
  description: string;
}
