import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { getSelectedSaleOutPrice } from './saleOutPrice';

describe('销售出库商品选中价格', () => {
  const product = { salePrice: 4.87, latestSalePrice: 9 };

  it('开启唯一售价配置时应回填商品售价', () => {
    expect(getSelectedSaleOutPrice(product, true)).toBe(4.87);
  });

  it('未开启唯一售价配置时应回填商品最新售价', () => {
    expect(getSelectedSaleOutPrice(product, false)).toBe(9);
  });

  it('各销售出库编辑页应按唯一售价配置回填商品价格', () => {
    for (const file of [
      './add-un-require.vue',
      './modify-un-require.vue',
      './add-require.vue',
      './modify-require.vue',
    ]) {
      const source = readFileSync(new URL(file, import.meta.url), 'utf-8');
      expect(source).toContain("import { getSelectedSaleOutPrice } from './saleOutPrice';");
      expect(source).toContain('getSelectedSaleOutPrice(product, this.useUniquePrice)');
      expect(source).toContain('this.useUniquePrice = await api.getPriceUniqueConfig();');
    }
  });
});
