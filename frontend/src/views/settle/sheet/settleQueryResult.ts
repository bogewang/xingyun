/** 结算工作台分页查询响应的最小结构。 */
interface SettleQueryResponse<T> {
  datas?: T[] | null;
  totalCount?: number | null;
}

/**
 * 将结算工作台分页响应转换为可安全消费的数据与总条数。
 *
 * @param response 后端分页响应
 * @returns 安全的数据数组与总条数
 */
export function normalizeSettleQueryResult<T>(response?: SettleQueryResponse<T> | null): {
  datas: T[];
  totalCount: number;
} {
  return {
    datas: Array.isArray(response?.datas) ? response.datas : [],
    totalCount:
      typeof response?.totalCount === 'number' && Number.isFinite(response.totalCount)
        ? response.totalCount
        : 0,
  };
}
