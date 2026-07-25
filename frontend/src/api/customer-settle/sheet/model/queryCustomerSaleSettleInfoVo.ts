import { SortPageVo } from '@/api/model/sortPageVo';

/** 客户销售结算工作台查询条件。 */
export interface QueryCustomerSaleSettleInfoVo extends SortPageVo {
  /** 单据号。 */
  code?: string;

  /** 客户 ID。 */
  customerId?: string;

  /** 业务类型：1-销售出库单，2-销售退货单。 */
  bizType: number;
}
