import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

describe('销售出库明细商品分类筛选', () => {
  it('支持多选末级商品分类，并将其加入查询条件', () => {
    const source = readFileSync(new URL('../detail-list.vue', import.meta.url), 'utf-8');

    expect(source).toContain('v-model:value="searchFormData.categoryIdList"');
    expect(source).toContain(':multiple="true"');
    expect(source).toContain(':only-final="true"');
    expect(source).toContain('categoryIdList: []');
  });
});
