export interface ReceiveProductVo {
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
   * 商品规格
   */
  spec: string;

  /**
   * 商品单位
   */
  unit: string;

  /**
   * 采购价
   */
  purchasePrice: number;

  /**
   * 收货数量
   */
  receiveNum: number;

  /**
   * 备注
   */
  description: string;

  /**
   * 采购订单明细ID
   */
  purchaseOrderDetailId: string;
}
