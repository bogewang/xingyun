/**
 * 商品批量更新可用状态请求参数。
 */
export interface UpdateProductAvailableVo {
  /** 商品 ID 列表。 */
  ids: string[];
  /** 目标可用状态。 */
  available: boolean;
}
