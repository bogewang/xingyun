export interface SaleOutSheetSelection {
  id?: string;
}

export interface MarketBuySummaryParams {
  idList: string[];
  groupByDate?: boolean;
}

/**
 * 根据勾选的销售出库单构建买菜汇总导出参数。
 * @param records 勾选的销售出库单列表
 * @param groupByDate 是否按日期汇总，默认不按日期汇总
 * @returns 包含单据ID和日期汇总选项的筛选条件
 */
export function buildMarketBuySummaryParams(
  records: SaleOutSheetSelection[],
  groupByDate = false,
): MarketBuySummaryParams {
  return {
    idList: (records || []).map((item) => item.id).filter((id): id is string => !!id),
    groupByDate,
  };
}

/**
 * 根据勾选的销售出库单构建买菜汇总2导出参数。
 * @param records 勾选的销售出库单列表
 * @returns 仅包含单据ID的汇总筛选条件
 */
export function buildMarketBuySummary2Params(
  records: SaleOutSheetSelection[],
): MarketBuySummaryParams {
  return {
    idList: (records || []).map((item) => item.id).filter((id): id is string => !!id),
  };
}
