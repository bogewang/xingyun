import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

const sheetPages = [
  '../sale/out/add-un-require.vue',
  '../sale/out/modify-un-require.vue',
  '../sale/out/add-require.vue',
  '../sale/out/modify-require.vue',
  '../purchase/receive/add-un-require.vue',
  '../purchase/receive/modify-un-require.vue',
  '../purchase/receive/add-require.vue',
  '../purchase/receive/modify-require.vue',
];

describe('出入库停用商品状态展示', () => {
  /** 验证停用商品优先显示停用状态，而非报价未匹配状态。 */
  it('商品停用时显示已停用', () => {
    sheetPages.forEach((file) => {
      const source = readFileSync(new URL(file, import.meta.url), 'utf-8');

      expect(source).toContain('row.available === false');
      expect(source).toContain('>已停用</a-tag>');
    });
  });
});
