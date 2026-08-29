import { describe, expect, it } from 'vitest';
import {
  applyQuoteProducts,
  filterQuoteProducts,
  hasInvalidQuoteProducts,
  normalizeQuoteProducts,
} from '../quoteProductPricing';

describe('报价商品定价', () => {
  const quoteProducts = [
    { productId: 'p-1', productCode: 'A001', productName: '商品A', salePrice: 12 },
    { productId: 'p-2', productCode: 'B001', productName: '商品B', salePrice: 20 },
  ];

  it('仅返回当前报价单中符合关键字的商品', () => {
    expect(filterQuoteProducts(quoteProducts, 'a001')).toEqual([quoteProducts[0]]);
    expect(filterQuoteProducts(quoteProducts, '商品B')).toEqual([quoteProducts[1]]);
  });

  it('将报价接口字段转换为销售选品字段', () => {
    const products = normalizeQuoteProducts([
      { productId: 'p-1', code: 'A001', name: '商品A', salePrice: 12 },
    ]);
    expect(products).toEqual([
      {
        productId: 'p-1',
        code: 'A001',
        name: '商品A',
        productCode: 'A001',
        productName: '商品A',
        salePrice: 12,
      },
    ]);
  });

  it('日期变化时保留可售商品、刷新价格并标记不可售商品', () => {
    const rows = applyQuoteProducts(
      [
        { productId: 'p-1', taxPrice: 10, conversionRate: 2 },
        { productId: 'p-3', taxPrice: 10 },
      ],
      quoteProducts,
    );

    expect(rows[0]).toMatchObject({ oriPrice: 12, baseSalePrice: 12, taxPrice: 24 });
    expect(rows[0].quoteInvalid).toBe(false);
    expect(rows[1].quoteInvalid).toBe(true);
    expect(hasInvalidQuoteProducts(rows)).toBe(true);
  });
});
