/** 报价单编辑页的数据组装。 */
export interface QuoteProductRow {
  productId: string;
  orderNo?: number;
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
    // 以表格有效商品的当前顺序明确传递排序号，避免保存链路中重新推断顺序。
    products: form.products.map((item: QuoteProductRow, index: number) => ({
      productId: item.productId,
      orderNo: index + 1,
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
