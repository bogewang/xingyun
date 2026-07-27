/**
 * 客户对账请求业务项 VO。
 */
export interface CustomerSettleCheckSheetItemVo {
  /** 业务单据ID。 */
  bizId: string;

  /** 业务类型：1-销售出库单，2-销售退货单。 */
  bizType: number;
}

/**
 * 客户对账单直接确认请求 VO。
 */
export interface CreateCustomerSettleCheckSheetVo {
  /** 客户ID。 */
  customerId: string;

  /** 确认对账金额。 */
  checkAmount: number;

  /** 备注。 */
  description?: string;

  /** 对账业务项。 */
  items: CustomerSettleCheckSheetItemVo[];
}
