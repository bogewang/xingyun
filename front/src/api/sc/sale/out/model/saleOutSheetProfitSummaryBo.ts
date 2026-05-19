export interface SaleOutSheetProfitSummaryBo {
  /**
   * 销售笔数
   */
  saleCount: number;

  /**
   * 销售额
   */
  salesAmount: number;

  /**
   * 成本
   */
  salesCost: number;

  /**
   * 销售毛利
   */
  salesProfit: number;

  /**
   * 其他收入
   */
  otherIncome: number;

  /**
   * 其他支出
   */
  otherExpense: number;

  /**
   * 净利润
   */
  netProfit: number;

  /**
   * 其他费用
   */
  otherFee: number;
}
