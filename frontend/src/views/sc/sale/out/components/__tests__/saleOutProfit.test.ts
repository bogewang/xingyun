import { describe, expect, it } from 'vitest';
import { calcSaleOutProfitRate } from '../saleOutProfit';

describe('saleOutProfit', () => {
  it('按验收金额减出库成本并除以出库金额计算毛利率', () => {
    expect(
      calcSaleOutProfitRate({
        taxPrice: 1.49,
        outNum: 100,
        confirmAmt: 147.5,
        costPrice: 1,
      }),
    ).toBe('31.88%');
  });
});
