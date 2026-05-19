import { SortPageVo } from '@/api/model/sortPageVo';

export interface QuerySaleOutSheetVo extends SortPageVo {
  /**
   * 单号
   */
  code: string;

  /**
   * 商品名称
   */
  productName: string;

  /**
   * 客户ID
   */
  customerId: string;

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
   * 销售员ID
   */
  salerId: string;

  /**
   * 销售订单号
   */
  saleOrderCode: string;

  /**
   * 结算状态
   */
  settleStatus: number;

  /**
   * 是否已付完
   */
  fullyPaid: boolean;

  /**
   * 是否已录采购
   */
  hasCostPrice: boolean;

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
