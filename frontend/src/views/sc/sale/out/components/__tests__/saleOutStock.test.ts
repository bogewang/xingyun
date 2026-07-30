import { describe, expect, it } from 'vitest';

import { isSaleOutStockEnough } from '../saleOutStock';

describe('saleOutStock', () => {
  it('数量首字符为小数点时不抛出 DecimalError', () => {
    const row = {
      productId: 'product-1',
      outNum: '.',
      stockNum: 10,
      baseStockNum: 10,
      conversionRate: 1,
    };

    expect(() => isSaleOutStockEnough([row], row)).not.toThrow();
    expect(isSaleOutStockEnough([row], row)).toBe(true);
  });

  it('有效小数仍按单位换算率累计库存', () => {
    const row = {
      productId: 'product-1',
      outNum: '.5',
      stockNum: 0.4,
      baseStockNum: 0.4,
      conversionRate: 1,
    };

    expect(isSaleOutStockEnough([row], row)).toBe(false);
  });
});
