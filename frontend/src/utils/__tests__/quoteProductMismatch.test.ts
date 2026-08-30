import { describe, expect, it } from 'vitest';
import { markQuoteProductMismatch } from '../quoteProductMismatch';

describe('报价商品未匹配标记', () => {
  it('按后端返回的商品行号标记对应的有效商品行', () => {
    const rows = [
      { productId: 'product-1', quoteUnmatched: false },
      { productId: '', quoteUnmatched: false },
      { productId: 'product-2', quoteUnmatched: false },
    ];

    markQuoteProductMismatch({ message: '第2行商品不在当前生效报价单中！' }, rows);

    expect(rows[0].quoteUnmatched).toBe(false);
    expect(rows[2].quoteUnmatched).toBe(true);
  });
});
