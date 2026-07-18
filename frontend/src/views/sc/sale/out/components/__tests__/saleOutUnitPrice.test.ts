import { describe, expect, it } from 'vitest';
import { calculateUnitPrice } from '../saleOutUnitPrice';

describe('saleOutUnitPrice', () => {
  it('切换单位时根据当前单位价和旧换算率计算新单位价', () => {
    const result = calculateUnitPrice(12, null, 2, 5);

    expect(result.basePrice).toBe(6);
    expect(result.unitPrice).toBe(30);
  });

  it('切换单位时优先复用已缓存的主单位价', () => {
    const result = calculateUnitPrice(999, 6, 2, 5);

    expect(result.basePrice).toBe(6);
    expect(result.unitPrice).toBe(30);
  });
});
