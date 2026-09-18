export interface MergeSaleOutSheetVo {
  /**
   * 销售出库单ID列表
   */
  ids: string[];

  /**
   * 合并后单据归属的客户ID；勾选单据包含多个客户时必填。
   */
  customerId?: string;

  /**
   * 用于确定合并后归属客户的已勾选销售出库单ID。
   */
  targetSheetId?: string;
}
