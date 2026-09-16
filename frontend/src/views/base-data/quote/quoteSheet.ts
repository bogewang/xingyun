/** 报价单编辑页的数据组装。 */
export interface QuoteProductRow {
  productId: string;
  orderNo?: number;
  code: string;
  name: string;
  displayName?: string;
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
    // 旧页面尚未接入项目切换器时，历史报价单继续归属默认项目。
    projectId: form.projectId || localStorage.getItem('xingyun-current-project-id') || 'default-project',
    name: form.name,
    startDate: form.startDate,
    endDate: form.endDate,
    description: form.description || '',
    // 以表格有效商品的当前顺序明确传递排序号，避免保存链路中重新推断顺序。
    products: form.products.map((item: QuoteProductRow, index: number) => ({
      productId: item.productId,
      displayName: item.displayName || item.name,
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
