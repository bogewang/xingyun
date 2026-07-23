import { QueryReceiveSheetBo } from '@/api/sc/purchase/receive/model/queryReceiveSheetBo';

/** 采购收货单对账结算扩展信息。 */
export interface QueryReceiveSheetSettleInfoBo extends QueryReceiveSheetBo {
  checkAmount: number;

  settleAmount: number;

  unSettleAmount?: number;

  checkDescription?: string;

  checkTime?: string;

  settleDescription?: string;

  settleTime?: string;
}
