import { CustomerSettleSheetItemVo } from '@/api/customer-settle/sheet/model/customerSettleSheetItemVo';

export interface CreateCustomerSettleSheetVo {
  /**
   * 客户ID
   */
  customerId: string;

  /** 项目。 */
  items: CustomerSettleSheetItemVo[];

  /** 确认结算金额。 */
  settleAmount: number;

  /** 备注。 */
  description?: string;

  /** 结算状态：1-部分结算，3-已结算。 */
  settleStatus?: number;
}
