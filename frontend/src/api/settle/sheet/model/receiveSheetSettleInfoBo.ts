import { QueryReceiveSheetBo } from '@/api/sc/purchase/receive/model/queryReceiveSheetBo';

export interface ReceiveSheetSettleInfoBo extends QueryReceiveSheetBo {
  checkAmount?: number;

  checkDescription?: string;

  checkTime?: string;

  settleAmount?: number;
  /**
   * 未结算金额
   */
  unSettleAmount?: number;

  settleDescription?: string;

  settleTime?: string;
}
