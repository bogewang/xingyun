import { describe, expect, it } from 'vitest';
import {
  calcSaleOutProfitAmount,
  calcSaleOutProfitRate,
  calcSaleOutProfitRateByCost,
  calcSaleOutProfitRateByProfit,
} from '../saleOutProfit';

describe('saleOutProfit', () => {
  it('毛利率按售价与成本单价计算，不受验收金额影响', () => {
    expect(
      calcSaleOutProfitRate({
        taxPrice: 1.49,
        outNum: 100,
        confirmAmt: 147.5,
        costPrice: 1,
      }),
    ).toBe('32.89%');
  });

  it('验收金额为零时毛利率仍按售价与成本单价计算', () => {
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

  it('验收金额为零时使用后端利润和销售金额计算毛利率', () => {
    expect(calcSaleOutProfitRateByProfit(10, 100, 0)).toBe('10.00%');
  });

  it('验收金额为零时列表毛利率使用销售金额减成本计算', () => {
    expect(calcSaleOutProfitRateByCost(100, 0, 20)).toBe('80.00%');
  });

  it('合计行使用逐单汇总后的毛利基数计算毛利率', () => {
    const profitBaseAmount = [
      { totalAmount: 100, confirmAmt: 0 },
      { totalAmount: 200, confirmAmt: 180 },
    ].reduce((total, row) => total + (row.confirmAmt !== 0 ? row.confirmAmt : row.totalAmount), 0);

    expect(calcSaleOutProfitRateByCost(profitBaseAmount, 0, 70)).toBe('75.00%');
  });

  it('编辑页毛利率不使用后端毛利字段', () => {
    expect(
      calcSaleOutProfitRate({
        taxPrice: 14.68,
        taxAmount: 14.68,
        outNum: 1,
        confirmAmt: 367,
        costPrice: 13.5,
        totalProfit: 353.5,
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
