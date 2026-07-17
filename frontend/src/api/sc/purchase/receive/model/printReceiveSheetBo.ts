export interface PrintReceiveSheetBo {
  /**
   * ID
   */
  id: string;

  /**
   * 单号
   */
  code: string;
  /**
   * 仓库ID
   */
  scId: string;
  /**
   * 仓库编号
   */
  scCode: string;
  /**
   * 仓库名称
   */
  scName: string;
  /**
   * 供应商编号
   */
  supplierCode: string;
  /**
   * 供应商ID
   */
  supplierId: string;
  /**
   * 供应商名称
   */
  supplierName: string;
  /**
   * 采购员姓名
   */
  purchaserName: string;
  /**
   * 采购员ID
   */
  purchaserId: string;
  /**
   * 订单日期
   */
  orderDate: string;
  /**
   * 付款日期
   */
  paymentDate: string;
  /**
   * 到货日期
   */
  receiveDate: string;
  /**
   * 采购订单ID
   */
  purchaseOrderId: string;
  /**
   * 采购订单号
   */
  purchaseOrderCode: string;
  /**
   * 备注
   */
  description: string;
  /**
   * 采购数量
   */
  totalNum: number;
  /**
   * 赠品数量
   */
  totalGiftNum: number;
  /**
   * 采购金额
   */
  totalAmount: number;
  /**
   * 已付金额
   */
  paidAmount: number;
  /**
   * 未付金额
   */
  unpaidAmount: number;
  /**
   * 创建人
   */
  createBy: string;
  /**
   * 创建时间
   */
  createTime: string;
  /**
   * 修改人
   */
  updateBy: string;
  /**
   * 修改时间
   */
  updateTime: string;
  /**
   * 审核人
   */
  approveBy: string;
  /**
   * 审核时间
   */
  approveTime: string;
  /**
   * 状态
   */
  status: number;
  /**
   * 拒绝原因
   */
  refuseReason: string;
  /**
   * 结算状态
   */
  settleStatus: number;
  /**
   * 订单明细
   */
  details: OrderDetailBo[];
}

export interface OrderDetailBo {
  /**
   * 明细ID
   */
  id: string;
  /**
   * 商品ID
   */
  productId: string;
  /**
   * 商品编号
   */
  productCode: string;
  /**
   * 商品名称
   */
  productName: string;
  /**
   * SKU编号
   */
  skuCode: string;
  /**
   * 简码
   */
  externalCode: string;
  /**
   * 收货数量原始值
   */
  orderNum: number;
  /**
   * 收货数量
   */
  receiveNum: number;
  /**
   * 单位ID
   */
  unitId: string;
  /**
   * 单位名称
   */
  unitName: string;
  /**
   * 换算率
   */
  conversionRate: number;
  /**
   * 业务数量
   */
  businessNum: number;
  /**
   * 采购价
   */
  purchasePrice: number;
  /**
   * 采购价原始值
   */
  taxPrice: number;
  /**
   * 收货金额
   */
  receiveAmount: number;
  /**
   * 采购总金额原始值
   */
  taxAmount: number;
  /**
   * 是否赠品
   */
  isGift: boolean;
  /**
   * 税率
   */
  taxRate: number;
  /**
   * 明细备注
   */
  description: string;
  /**
   * 排序编号
   */
  orderNo: number;
  /**
   * 采购订单明细ID
   */
  purchaseOrderDetailId: string;
  /**
   * 生产日期
   */
  productionDate: string;
  /**
   * 规格
   */
  spec: string;

  seq: number;
}
