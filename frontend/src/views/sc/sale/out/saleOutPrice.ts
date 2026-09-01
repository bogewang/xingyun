/** 销售出库可选商品的价格字段。 */
export interface SaleOutProductPrice {
  salePrice?: number | string | null;
  latestSalePrice?: number | string | null;
}

/**
 * 获取销售出库单选中商品时应回填的售价。
 * 开启唯一售价配置时使用商品售价，否则使用商品最新售价。
 *
 * @param product 商品价格信息
 * @param useUniquePrice 是否开启唯一售价配置
 * @returns 用于回填的销售价格
 */
export function getSelectedSaleOutPrice(
  product: SaleOutProductPrice,
  useUniquePrice: boolean,
): number | string | null | undefined {
  return useUniquePrice ? product.salePrice : product.latestSalePrice;
}
