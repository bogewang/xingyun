/** 客户结算总览行数据。 */
export interface CustomerSettleOverviewBo {
  /** 客户 ID。 */
  customerId: string;

  /** 客户编号。 */
  customerCode: string;

  /** 客户名称。 */
  customerName: string;

  /** 待对账单据数。 */
  unCheckCount: number;

  /** 待对账金额。 */
  unCheckAmount: number;

  /** 待结算单据数。 */
  unSettleCount: number;

  /** 待结算金额。 */
  unSettleAmount: number;

  /** 部分结算单据数。 */
  partSettleCount: number;

  /** 部分结算金额。 */
  partSettleAmount: number;

  /** 已结算单据数。 */
  settledCount: number;

  /** 已结算金额。 */
  settledAmount: number;
}
