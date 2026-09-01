/** 报价单编辑页的数据组装。 */
export interface QuoteProductRow {
  productId: string;
  code: string;
  name: string;
  shortName?: string;
  skuCode?: string;
  spec?: string;
  unit?: string;
  salePrice: string | number;
  inquiryProduct?: boolean;
}

/** 构造后端报价单保存请求。 */
export function buildQuoteSheetPayload(form: Record<string, any>) {
  return {
    ...(form.id ? { id: form.id } : {}),
    name: form.name,
    startDate: form.startDate,
    endDate: form.endDate,
    description: form.description || '',
    products: form.products.map((item: QuoteProductRow) => ({
      productId: item.productId,
      salePrice: item.salePrice,
      inquiryProduct: item.inquiryProduct !== false,
    })),
  };
}

/** 将报价商品中的单位 ID 转换为可读的单位名称，兼容历史名称数据。 */
export function resolveQuoteProductUnitName(
  unit: string | undefined,
  unitNameMap: Record<string, string>,
) {
  return unitNameMap[unit || ''] || unit;
}
