/** 当前销售日期有效报价单中的商品。 */
export interface QuoteProductBo {
  /** 商品 ID */
  productId: string;
  /** 报价销售价 */
  salePrice: number;
  /** 后端返回的商品编号 */
  code?: string;
  /** 后端返回的商品名称 */
  name?: string;
  skuCode?: string;
  externalCode?: string;
  /** 页面展示用的商品编号 */
  productCode?: string;
  /** 页面展示用的商品名称 */
  productName?: string;
  [key: string]: unknown;
}
