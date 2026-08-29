/** 报价商品最小数据结构。报价单 ID 仅由后端保存到销售单主表，前端不传递该字段。 */
export interface QuoteProduct {
  productId: string;
  salePrice: number | string;
  [key: string]: unknown;
}

/** 将报价接口的 code/name 字段转换为销售选品组件的标准字段。 */
export function normalizeQuoteProducts<T extends QuoteProduct>(products: T[]): T[] {
  return products.map((product) => {
    const { quoteSheetId: _quoteSheetId, ...visibleProduct } = product as T & {
      quoteSheetId?: string;
    };
    return {
      ...visibleProduct,
      productCode: product.productCode || product.code || '',
      productName: product.productName || product.name || '',
    };
  }) as T[];
}

/** 按关键字过滤当前报价单商品。 */
export function filterQuoteProducts<T extends QuoteProduct>(products: T[], keyword: string): T[] {
  const normalizedKeyword = keyword.trim().toLowerCase();
  if (!normalizedKeyword) {
    return products;
  }

  return products.filter((product) =>
    [product.productCode, product.productName, product.skuCode, product.externalCode]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(normalizedKeyword)),
  );
}

/**
 * 将当前报价单价格应用到已有销售明细；不在报价单中的商品保留并标记，交由页面阻止保存。
 */
export function applyQuoteProducts<T extends Record<string, any>>(
  rows: T[],
  quoteProducts: QuoteProduct[],
) {
  const quoteProductMap = new Map(quoteProducts.map((product) => [product.productId, product]));
  return rows.map((row) => {
    if (!row.productId) {
      return { ...row, quoteInvalid: false };
    }

    const quoteProduct = quoteProductMap.get(row.productId);
    if (!quoteProduct) {
      return { ...row, quoteInvalid: true };
    }

    const basePrice = Number(quoteProduct.salePrice);
    const conversionRate = Number(row.conversionRate) || 1;
    return {
      ...row,
      oriPrice: basePrice,
      baseSalePrice: basePrice,
      taxPrice: basePrice * conversionRate,
      quoteInvalid: false,
    };
  });
}

/** 判断当前明细是否存在不属于有效报价单的商品。 */
export function hasInvalidQuoteProducts(rows: Array<Record<string, any>>): boolean {
  return rows.some((row) => !!row.productId && row.quoteInvalid === true);
}
