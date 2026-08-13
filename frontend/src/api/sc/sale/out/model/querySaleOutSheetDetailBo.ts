export interface QuerySaleOutSheetDetailBo {
  id: string;
  code: string;
  customerId: string;
  customerCode: string;
  customerName: string;
  orderDate: string;
  saleOrderId: string;
  saleOrderCode: string;
  detailId: string;
  productId: string;
  productCode: string;
  productName: string;
  /** 商品备注 */
  productRemark: string;
  /** 是否询价商品 */
  inquiryProduct: boolean | null;
  skuCode: string;
  externalCode: string;
  spec: string;
  unit: string;
  categoryName: string;
  brandName: string;
  orderNum: number;
  taxPrice: number;
  taxAmount: number;
  /** 验收数量 */
  confirmNum: number;
  /** 验收金额 */
  confirmAmt: number;
  costPrice: number;
  totalProfit: number;
  isGift: boolean;
  description: string;
  settleStatus: number;
  createBy: string;
  createTime: string;
  approveBy: string;
  approveTime: string;
  status: number;
}
