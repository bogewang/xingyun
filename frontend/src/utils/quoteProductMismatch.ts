/**
 * 从报价商品校验异常中提取未匹配的明细行号。
 */
export function getQuoteProductMismatchRows(error: unknown): number[] {
  const response = (error || {}) as {
    msg?: unknown;
    message?: unknown;
    error?: { message?: unknown };
  };
  const message = String(response.msg || response.message || response.error?.message || '');
  return Array.from(message.matchAll(/第(\d+)行商品不在当前生效报价单中/g)).map((match) =>
    Number(match[1]),
  );
}

/**
 * 标记报价单未包含的商品行，供商品编号列显示“未匹配”。
 */
export function markQuoteProductMismatch(error: unknown, rows: Record<string, any>[]) {
  rows.forEach((row) => {
    row.quoteUnmatched = false;
  });
  getQuoteProductMismatchRows(error).forEach((rowNumber) => {
    const row = rows.filter((item) => !!item.productId)[rowNumber - 1];
    if (row) {
      row.quoteUnmatched = true;
    }
  });
}

/**
 * 根据当前订单日期的报价商品标记表格中未匹配的商品。
 */
export function markProductsOutsideQuoteSheet(
  rows: Record<string, any>[],
  quoteProducts: {
    productId: string;
    code?: string;
    name?: string;
    spec?: string;
    unit?: string;
  }[],
  enabled: boolean,
) {
  const quoteProductIds = new Set(quoteProducts.map((item) => item.productId));
  rows.forEach((row) => {
    const matchedProducts = quoteProducts.filter(
      (item) =>
        !row.productId &&
        !!row.productName &&
        item.name === row.productName &&
        (!row.unit || !item.unit || item.unit === row.unit),
    );
    if (enabled && matchedProducts.length === 1) {
      const product = matchedProducts[0];
      Object.assign(row, {
        productId: product.productId,
        productCode: product.code || row.productCode,
        productName: product.name || row.productName,
        spec: product.spec || row.spec,
        unit: product.unit || row.unit,
        importUnmatched: false,
      });
    }
    row.quoteUnmatched = !!row.productId && !quoteProductIds.has(row.productId);
  });
}
