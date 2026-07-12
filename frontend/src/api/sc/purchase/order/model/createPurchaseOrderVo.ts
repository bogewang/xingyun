import { PurchaseProductVo } from '@/api/sc/purchase/order/model/purchaseProductVo';

export interface CreatePurchaseOrderVo {
  /**
   * 仓库ID
   */
  scId: string;

  /**
   * 供应商ID
   */
  supplierId: string;

  /**
   * 采购员ID
   */
  purchaserId: string;

  /**
   * 订单日期
   */
  orderDate: string;

  /**
   * 商品信息
   */
  products: PurchaseProductVo[];

  /**
   * 备注
   */
  description: string;
}
