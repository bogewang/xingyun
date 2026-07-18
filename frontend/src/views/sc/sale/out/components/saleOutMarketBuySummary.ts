export interface SaleOutSheetSelection {
  id?: string;
}

export interface MarketBuySummaryParams {
  idList: string[];
}

/**
 * 根据勾选的销售出库单构建买菜汇总导出参数。
 * @param records 勾选的销售出库单列表
 * @returns 仅包含单据ID的汇总筛选条件
 */
export function buildMarketBuySummaryParams(
  records: SaleOutSheetSelection[],
): MarketBuySummaryParams {
  return {
    idList: (records || []).map((item) => item.id).filter((id): id is string => !!id),
  };
}
