export interface SaleOutProductVo {
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
   * 原价
   */
  oriPrice: number;

  /**
   * 现价
   */
  taxPrice: number;

  /**
   * 折扣（%）
   */
  discountRate: number;

  /**
   * 出库数量
   */
  orderNum: number;

  /**
   * 验收数量
   */
  confirmNum: number;

  /**
   * 备注
   */
  description: string;

  /**
   * 是否询价商品
   */
  inquiryProduct: boolean;

  /**
   * 成本单价
   */
  costPrice: number;

  /**
   * 销售订单明细ID
   */
  saleOrderDetailId: string;

  /**
   * 计划日期
   */
  planDate?: string;
}
