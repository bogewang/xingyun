export interface QueryReceiveSheetDetailBo {
  id: string;
  code: string;
  supplierId: string;
  supplierCode: string;
  supplierName: string;
  orderDate: string;
  receiveDate: string;
  purchaseOrderId: string;
  purchaseOrderCode: string;
  detailId: string;
  productId: string;
  productCode: string;
  productName: string;
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
  isGift: boolean;
  productionDate: string;
  description: string;
  settleStatus: number;
  createBy: string;
  createTime: string;
  approveBy: string;
  approveTime: string;
  status: number;
}
