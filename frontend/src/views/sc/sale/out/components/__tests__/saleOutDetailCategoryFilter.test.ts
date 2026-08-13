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

  it('支持勾选销售明细后打印标签', () => {
    const source = readFileSync(new URL('../detail-list.vue', import.meta.url), 'utf-8');

    expect(source).toContain("{ type: 'checkbox', width: 45 }");
    expect(source).toContain('@click="tagPrint"');
    expect(source).toContain('detailIdList: records.map((item) => item.detailId)');
    expect(source).toContain('this.vgPrintPreview(PRINT_TYPE.SALE_TAG.code, res)');
  });
});
