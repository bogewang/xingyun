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
