import { SortPageVo } from '@/api/model/sortPageVo';

export interface QueryReceiveSheetVo extends SortPageVo {
  /**
   *
   */
  serialVersionUID: long;

  /**
   * 单号
   */
  code: string;

  /**
   * 单据备注
   */
  sheetDescription: string;

  /**
   * 商品名称
   */
  productName: string;

  /**
   * 供应商ID
   */
  supplierId: string;

  /**
   * 仓库ID
   */
  scId: string;

  /**
   * 操作人ID
   */
  createBy: string;

  /**
   * 订单起始日期
   */
  orderDateStart: string;

  /**
   * 订单截止日期
   */
  orderDateEnd: string;

  /**
   * 审核人ID
   */
  approveBy: string;

  /**
   * 审核起始时间
   */
  approveStartTime: string;

  /**
   * 审核截止时间
   */
  approveEndTime: string;

  /**
   * 状态
   */
  status: number;

  /**
   * 采购员ID
   */
  purchaserId: string;

  /**
   * 采购订单号
   */
  purchaseOrderCode: string;

  /**
   * 结算状态
   */
  settleStatus: number;

  /**
   * 是否已付完
   */
  fullyPaid: boolean;

  /**
   * 已付金额起始值
   */
  paidAmountStart: number;

  /**
   * 已付金额截止值
   */
  paidAmountEnd: number;

  /**
   * 未付金额起始值
   */
  unpaidAmountStart: number;

  /**
   * 未付金额截止值
   */
  unpaidAmountEnd: number;
}
