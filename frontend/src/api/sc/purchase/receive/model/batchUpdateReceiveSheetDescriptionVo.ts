/** 批量更新采购收货单备注参数。 */
export interface BatchUpdateReceiveSheetDescriptionVo {
  /** 收货单ID列表。 */
  ids: string[];

  /** 备注。 */
  description?: string;
}
