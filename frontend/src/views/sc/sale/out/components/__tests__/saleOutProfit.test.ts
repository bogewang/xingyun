import { describe, expect, it } from 'vitest';
import {
  calcSaleOutProfitAmount,
  calcSaleOutProfitRate,
  calcSaleOutProfitRateByProfit,
} from '../saleOutProfit';

describe('saleOutProfit', () => {
  it('验收金额不为零时按验收金额计算毛利率', () => {
    expect(
      calcSaleOutProfitRate({
        taxPrice: 1.49,
        outNum: 100,
        confirmAmt: 147.5,
        costPrice: 1,
      }),
    ).toBe('32.20%');
  });

  it('验收金额为零时按销售金额计算毛利率', () => {
    expect(
      calcSaleOutProfitRate({
        taxPrice: 1.49,
        outNum: 100,
        confirmAmt: 0,
        costPrice: 1,
      }),
    ).toBe('32.89%');
  });

  it('验收金额非零时使用后端利润和验收金额计算毛利率', () => {
    expect(calcSaleOutProfitRateByProfit(31.55, 7.61, 38.05)).toBe('82.92%');
  });

  it('编辑页优先使用后端返回的毛利计算毛利率', () => {
    expect(
      calcSaleOutProfitRate({
        taxAmount: 14.68,
        outNum: 1,
        confirmAmt: 367,
        costPrice: 13.5,
        totalProfit: 29.5,
      } as any),
    ).toBe('8.04%');
  });

  it('存在验收数量时按验收数量计算成本和毛利', () => {
    expect(
      calcSaleOutProfitAmount({
        taxAmount: 14.68,
        outNum: 1,
        confirmNum: 25,
        confirmAmt: 367,
        costPrice: 13.5,
      } as any),
    ).toBe(29.5);
  });
});
