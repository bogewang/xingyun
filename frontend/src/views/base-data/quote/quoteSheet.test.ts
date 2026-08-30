import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { buildQuoteSheetPayload } from './quoteSheet';

describe('报价单编辑数据', () => {
  it('列表页必须启用分页配置，以解析分页响应中的 datas', () => {
    const source = readFileSync(new URL('./index.vue', import.meta.url), 'utf-8');
    expect(source).toContain(':pager-config');
  });

  it('保存时只提交报价单接口需要的有效期和商品单价字段，不包含编号', () => {
    expect(
      buildQuoteSheetPayload({
        id: 'q1',
        name: '九月报价',
        startDate: '2026-09-01',
        endDate: '2026-09-30',
        description: '测试',
        status: 'ENABLED',
        products: [{ productId: 'p1', code: 'P001', name: '商品', salePrice: '12.50' }],
      }),
    ).toEqual({
      id: 'q1',
      name: '九月报价',
      startDate: '2026-09-01',
      endDate: '2026-09-30',
      description: '测试',
      products: [{ productId: 'p1', salePrice: '12.50' }],
    });
  });

  it('编辑页必须通过下拉框提供报价单状态切换入口', () => {
    const source = readFileSync(new URL('./modify.vue', import.meta.url), 'utf-8');

    expect(source).toContain('@change="changeStatus"');
    expect(source).toContain('<a-select-option value="ENABLED">启用</a-select-option>');
    expect(source).toContain('<a-select-option value="DISABLED">停用</a-select-option>');
    expect(source).toContain('api.enable');
    expect(source).toContain('api.disable');
  });
});
