import { describe, expect, it } from 'vitest';
import { markProductsOutsideQuoteSheet, markQuoteProductMismatch } from '../quoteProductMismatch';

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

  it('切换到无报价单日期时标记已选商品未匹配', () => {
    const rows = [
      { productId: 'product-1', quoteUnmatched: true },
      { productId: 'product-2', quoteUnmatched: false },
    ];

    markProductsOutsideQuoteSheet(rows, [{ productId: 'product-1' }], true);
    expect(rows.map((row) => row.quoteUnmatched)).toEqual([false, true]);

    markProductsOutsideQuoteSheet(rows, [], false);
    expect(rows.map((row) => row.quoteUnmatched)).toEqual([true, true]);
  });

  it('导入时未匹配的商品切换日期后命中报价单，会回填商品并恢复正常显示', () => {
    const rows = [
      {
        productId: '',
        productCode: '',
        productName: '紫甘蓝',
        importUnmatched: true,
        quoteUnmatched: true,
      },
    ];

    markProductsOutsideQuoteSheet(
      rows,
      [{ productId: 'product-purple-cabbage', code: 'P001', name: '紫甘蓝', unit: '公斤' }],
      true,
    );

    expect(rows[0]).toMatchObject({
      productId: 'product-purple-cabbage',
      productCode: 'P001',
      importUnmatched: false,
      quoteUnmatched: false,
    });
  });
});
