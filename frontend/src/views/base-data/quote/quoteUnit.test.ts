import { describe, expect, it } from 'vitest';
import { applyQuoteUnit, expandQuoteUnits, isValidQuoteRate, quoteUnitOptionValue } from './quoteUnit';
import { buildQuoteSheetPayload } from './quoteSheet';

describe('报价单位选择', () => {
  it('包袋和千克公斤kg互认，沿用源单位换算率和ID', () => {
    const options = expandQuoteUnits([
      { id: 'bag', unitName: '袋', conversionRate: '1.00' },
      { id: 'box', unitName: '件', conversionRate: 20 },
      { id: 'weight', unitName: '公斤', conversionRate: 2 },
    ]);
    expect(options.map((unit) => unit.unitName)).toEqual(['袋', '件', '公斤', '包', '千克', 'kg']);
    expect(options.find((unit) => unit.unitName === '包')).toMatchObject({ id: 'bag', conversionRate: '1.00' });
    expect(options.find((unit) => unit.unitName === 'kg')).toMatchObject({ id: 'weight', conversionRate: 2 });
    expect(quoteUnitOptionValue(options[0])).not.toBe(quoteUnitOptionValue(options[3]));
  });

  it('商品明确配置的同名单位优先，停用单位不派生别名', () => {
    const options = expandQuoteUnits([
      { id: 'bag', unitName: '袋', conversionRate: 1 },
      { id: 'package', unitName: '包', conversionRate: 2 },
      { id: 'disabled', unitName: '千克', conversionRate: 1, available: false },
    ]);
    expect(options.map((unit) => unit.unitName)).toEqual(['袋', '包']);
    expect(options.find((unit) => unit.unitName === '包')?.id).toBe('package');
  });

  it('切换单位同步保存ID、名称及换算率，不修改项目品名、规格和价格', () => {
    const row = { productId: 'p1', name: '项目品名', spec: '', salePrice: 20, unit: '袋' };
    applyQuoteUnit(row, { id: 'u2', unitName: '件', conversionRate: '12.50' });
    const payload = buildQuoteSheetPayload({ projectId: 'project-a', products: [row] });
    expect(payload.products[0]).toMatchObject({
      unitId: 'u2', unitName: '件', conversionRate: 12.5,
      productName: '项目品名', spec: '', salePrice: 20,
    });
  });

  it('拒绝无效换算率和超过两位的有效精度，兼容数据库末尾补零', () => {
    for (const rate of [1, '0.01', '12.50000000']) expect(isValidQuoteRate(rate)).toBe(true);
    for (const rate of [0, -1, null, '', '0.001', '1.234', 'abc']) {
      expect(isValidQuoteRate(rate)).toBe(false);
    }
  });
});
