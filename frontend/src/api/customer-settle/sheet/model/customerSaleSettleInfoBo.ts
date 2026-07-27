/** 客户销售结算工作台行数据。 */
export interface CustomerSaleSettleInfoBo {
  /** 单据 ID。 */
  id: string;

  /** 业务类型：1-销售出库单，2-销售退货单。 */
  bizType: number;

  /** 单据号。 */
  code: string;

  /** 客户 ID。 */
  customerId: string;

  /** 客户名称。 */
  customerName: string;

  /** 应收金额。 */
  totalAmount: number;

  /** 已收金额。 */
  receivedAmount: number;

  /** 已对账金额。 */
  checkAmount?: number;

  /** 对账时间。 */
  checkTime?: string;

  /** 对账备注。 */
  checkDescription?: string;

  /** 已结算金额。 */
  settleAmount: number;

  /** 未结算金额。 */
  unSettleAmount: number;

  /** 结算状态。 */
  settleStatus: number;
}
