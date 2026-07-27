import { SortPageVo } from '@/api/model/sortPageVo';

/** 客户结算总览查询条件。 */
export interface QueryCustomerSettleOverviewVo extends SortPageVo {
  /** 客户 ID。 */
  customerId?: string;
}
