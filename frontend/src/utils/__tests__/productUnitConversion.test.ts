import { describe, expect, it } from 'vitest';
import {
  calculateUnitPrice,
  calculateUnitStockNum,
  getUnitConversionRate,
} from '../productUnitConversion';

describe('productUnitConversion', () => {
  it('切换单位时根据当前库存和旧换算率反算主单位库存', () => {
    const result = calculateUnitStockNum(50, null, 2, 5);

    expect(result.baseStockNum).toBe(100);
    expect(result.stockNum).toBe(20);
  });

  it('切换单位时优先复用已缓存的主单位库存', () => {
    const result = calculateUnitStockNum(50, 100, 2, 5);

    expect(result.baseStockNum).toBe(100);
    expect(result.stockNum).toBe(20);
  });

  it('主单位库存为空字符串时根据当前库存反算', () => {
    const result = calculateUnitStockNum(50, '', 2, 5);

    expect(result.baseStockNum).toBe(100);
    expect(result.stockNum).toBe(20);
  });

  it('切换单位时根据当前单位价和旧换算率计算新单位价', () => {
    const result = calculateUnitPrice(12, null, 2, 5);

    expect(result.basePrice).toBe(6);
    expect(result.unitPrice).toBe(30);
  });

  it('主单位价为空字符串时根据当前单位价反算', () => {
    const result = calculateUnitPrice(12, '', 2, 5);

    expect(result.basePrice).toBe(6);
    expect(result.unitPrice).toBe(30);
  });

  it('获取单位换算率时优先使用当前行换算率', () => {
    const result = getUnitConversionRate({
      conversionRate: 3,
      unitId: 'box',
      units: [{ id: 'box', conversionRate: 5 }],
    });

    expect(result).toBe(3);
  });

  it('当前行换算率为空时从单位列表获取换算率', () => {
    const result = getUnitConversionRate({
      conversionRate: '',
      unitId: 'box',
      units: [{ id: 'box', conversionRate: 5 }],
    });

    expect(result).toBe(5);
  });
});
