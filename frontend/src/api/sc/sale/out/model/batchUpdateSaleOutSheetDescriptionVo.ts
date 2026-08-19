/** 批量更新销售出库单备注参数。 */
export interface BatchUpdateSaleOutSheetDescriptionVo {
  /** 销售出库单ID列表。 */
  ids: string[];

  /** 备注。 */
  description?: string;
}
