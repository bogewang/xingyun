import { describe, expect, it } from 'vitest';
import { buildQuoteSheetPayload, mergeQuoteProducts } from './quoteSheet';

describe('报价单编辑数据', () => {
  it('批量添加商品时保留既有商品并过滤重复商品', () => {
    expect(
      mergeQuoteProducts(
        [{ productId: 'p1', code: 'A', name: '已有商品', salePrice: 10 }],
        [
          { productId: 'p1', productCode: 'A', productName: '已有商品', salePrice: 99 },
          { productId: 'p2', productCode: 'B', productName: '新商品', salePrice: 20 },
        ],
      ),
    ).toEqual([
      { productId: 'p1', code: 'A', name: '已有商品', salePrice: 10 },
      { productId: 'p2', code: 'B', name: '新商品', salePrice: 20 },
    ]);
  });

  it('保存时只提交报价单接口需要的有效期和商品单价字段', () => {
    expect(
      buildQuoteSheetPayload({
        id: 'q1',
        code: 'QT001',
        name: '九月报价',
        startDate: '2026-09-01',
        endDate: '2026-09-30',
        description: '测试',
        status: 'ENABLED',
        products: [{ productId: 'p1', code: 'P001', name: '商品', salePrice: '12.50' }],
      }),
    ).toEqual({
      id: 'q1',
      code: 'QT001',
      name: '九月报价',
      startDate: '2026-09-01',
      endDate: '2026-09-30',
      description: '测试',
      products: [{ productId: 'p1', salePrice: '12.50' }],
    });
  });
});
