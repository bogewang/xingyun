/**
 * 供应商批量更新可用状态请求参数。
 */
export interface UpdateSupplierAvailableVo {
  /** 供应商 ID 列表。 */
  ids: string[];
  /** 目标可用状态。 */
  available: boolean;
}
