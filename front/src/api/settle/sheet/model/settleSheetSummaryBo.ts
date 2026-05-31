export interface SettleSheetSummaryBo {
  supplierId: string;
  supplierCode: string;
  supplierName: string;
  /**
   * 未对账单数量
   */
  unCheckSheetNum: number;
  /**
   * 未对账单总金额
   */
  unCheckTotalAmount: number;
  /**
   * 未结算单数量
   */
  unSettleSheetNum: number;
  /**
   * 未结算单总金额
   */
  unSettleTotalAmount: number;
  /**
   * 部份结算单数量
   */
  partSettleSheetNum: number;
  /**
   * 部份结算单总金额
   */
  partSettleTotalAmount: number;
  /**
   * 已结算单数量
   */
  settledSheetNum: number;
  /**
   * 已结算单总金额
   */
  settledTotalAmount: number;
}
