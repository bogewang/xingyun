import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import {
  buildRequiredSaleOutProducts,
  buildUnrequiredSaleOutProducts,
} from '../saleOutProductParams';

const saleOutDirectory = resolve(__dirname, '..', '..');

/**
 * 读取销售出库页面源代码，验证询价商品展示列已配置。
 */
function readSaleOutSource(relativePath: string) {
  return readFileSync(resolve(saleOutDirectory, relativePath), 'utf-8');
}

describe('销售出库询价商品展示', () => {
  it('销售出库明细列包含是否询价商品', () => {
    const sources = [
      'add-require.vue',
      'add-un-require.vue',
      'modify-require.vue',
      'modify-un-require.vue',
      'detail.vue',
      'components/detail-list.vue',
    ].map(readSaleOutSource);

    sources.forEach((source) => {
      expect(source).toContain("field: 'inquiryProduct'");
      expect(source).toContain('formatInquiryProduct');
    });
  });

  it('销售批量添加商品弹窗包含是否询价商品', () => {
    const source = readSaleOutSource('../batch-add-product.vue');

    expect(source).toContain(':show-inquiry-product="true"');
  });

  it('保存销售出库明细时不携带是否询价商品', () => {
    const unrequiredRows = [
      {
        productId: 'product-1',
        unitId: 'unit-1',
        outNum: 1,
        taxPrice: 10,
        inquiryProduct: true,
      },
    ];
    const requiredRows = [
      {
        id: 'detail-1',
        productId: 'product-1',
        unitId: 'unit-1',
        outNum: 1,
        taxPrice: 10,
        inquiryProduct: true,
      },
    ];
    const [unrequiredProduct] = buildUnrequiredSaleOutProducts(unrequiredRows);
    const [requiredProduct] = buildRequiredSaleOutProducts(requiredRows);

    expect(unrequiredProduct).not.toHaveProperty('inquiryProduct');
    expect(requiredProduct).not.toHaveProperty('inquiryProduct');
  });
});
