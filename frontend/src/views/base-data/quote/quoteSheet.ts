/** 报价单编辑页的数据组装与商品去重。 */
export interface QuoteProductRow {
  productId: string;
  code: string;
  name: string;
  shortName?: string;
  skuCode?: string;
  spec?: string;
  unit?: string;
  salePrice: string | number;
}

/** 合并批量选择的商品，已存在商品保留原报价。 */
export function mergeQuoteProducts(
  current: QuoteProductRow[],
  selected: Array<Record<string, any>>,
): QuoteProductRow[] {
  const productIds = new Set(current.map((item) => item.productId));
  return current.concat(
    selected
      .filter((item) => !productIds.has(item.productId || item.id))
      .map((item) => ({
        productId: item.productId || item.id,
        code: item.productCode || item.code,
        name: item.productName || item.name,
        shortName: item.shortName,
        skuCode: item.skuCode,
        spec: item.spec,
        unit: item.unit,
        salePrice: item.salePrice ?? 0,
      })),
  );
}

/** 构造后端报价单保存请求。 */
export function buildQuoteSheetPayload(form: Record<string, any>) {
  return {
    ...(form.id ? { id: form.id } : {}),
    code: form.code,
    name: form.name,
    startDate: form.startDate,
    endDate: form.endDate,
    description: form.description || '',
    products: form.products.map((item: QuoteProductRow) => ({
      productId: item.productId,
      salePrice: item.salePrice,
    })),
  };
}
