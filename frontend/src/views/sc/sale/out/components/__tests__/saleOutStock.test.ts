import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';

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

  it('大数库存比较不会因转成 JavaScript number 而丢失小数精度', () => {
    const row = {
      productId: 'product-1',
      outNum: '9007199254740992.1',
      stockNum: '9007199254740992',
      baseStockNum: '9007199254740992',
      conversionRate: 1,
    };

    expect(isSaleOutStockEnough([row], row)).toBe(false);
  });

  it('空白数量不会进入 Decimal 库计算', () => {
    const row = {
      productId: 'product-1',
      outNum: ' ',
      stockNum: 10,
      baseStockNum: 10,
      conversionRate: 1,
    };

    expect(() => isSaleOutStockEnough([row], row)).not.toThrow();
    expect(isSaleOutStockEnough([row], row)).toBe(true);
  });

  it('销售出库修改页复用安全的库存计算', () => {
    const source = readFileSync(new URL('../../modify-un-require.vue', import.meta.url), 'utf-8');

    expect(source).toContain('return isSaleOutStockEnough(this.tableData, row);');
  });
});
