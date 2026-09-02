import { SortPageVo } from '@/api/model/sortPageVo';

export interface QuerySaleOutSheetVo extends SortPageVo {
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
   * 明细备注
   */
  description: string;

  /**
   * 商品ID
   */
  productId: string;

  /**
   * 规格型号
   */
  productSpec: string;

  /**
   * 单据ID列表
   */
  idList: string[];

  /**
   * 客户ID
   */
  customerId: string;

  /**
   * 客户ID列表
   */
  customerIdList: string[];

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
   * 计划起始日期
   */
  planDateStart: string;

  /**
   * 计划截止日期
   */
  planDateEnd: string;

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
   * 是否已收完款
   */
  fullyPaid: boolean;

  /**
   * 是否询价商品
   */
  inquiryProduct: boolean;

  /**
   * 是否已录采购
   */
  hasCostPrice: boolean;

  /**
   * 是否仅查询负毛利商品
   */
  onlyNegativeProfit: boolean;

  /**
   * 是否录完所有成本
   */
  fillAllCost: boolean;

  /**
   * 是否已送货
   */
  delivered: boolean;

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

  /**
   * 是否仅查询多价格商品
   */
  onlyMultiPrice: boolean;

  /**
   * 是否存在金额为0的明细
   */
  hasZeroAmountDetail: boolean;

  /**
   * 商品分类ID列表
   */
  categoryIdList: string[];
}
