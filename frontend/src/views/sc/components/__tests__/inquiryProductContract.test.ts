import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const scDirectory = resolve(__dirname, '..', '..');
const frontendSrcDirectory = resolve(scDirectory, '..', '..');

/**
 * 读取前端 src 目录下的源码。
 */
function readSource(relativePath: string) {
  return readFileSync(resolve(frontendSrcDirectory, relativePath), 'utf-8');
}

describe('询价商品前端契约', () => {
  it('商品联想和共享批量列表实际使用销售商品端点', () => {
    const apiSource = readSource('api/sc/sale/order/index.ts');
    const receiveSource = readSource('views/sc/purchase/receive/add-un-require.vue');
    const saleOutSource = readSource('views/sc/sale/out/add-un-require.vue');
    const sharedBatchSource = readSource('views/sc/shared/batch-add-product.vue');

    expect(apiSource).toContain("baseUrl + '/product/search'");
    expect(apiSource).toContain("baseUrl + '/product/list'");
    expect(receiveSource).toContain('saleApi.searchSaleProducts');
    expect(saleOutSource).toContain('saleApi.searchSaleProducts');
    expect(sharedBatchSource).toContain('saleApi.querySaleProductList');
  });

  it('批量添加询价列默认关闭且仅采购收货与销售出库显式开启', () => {
    const purchaseWrapper = readSource('views/sc/purchase/batch-add-product.vue');
    const saleWrapper = readSource('views/sc/sale/batch-add-product.vue');
    const sharedBatchSource = readSource('views/sc/shared/batch-add-product.vue');
    const enabledCallers = [
      'views/sc/purchase/receive/add-require.vue',
      'views/sc/purchase/receive/add-un-require.vue',
      'views/sc/purchase/receive/modify-require.vue',
      'views/sc/purchase/receive/modify-un-require.vue',
      'views/sc/sale/out/add-require.vue',
      'views/sc/sale/out/add-un-require.vue',
      'views/sc/sale/out/modify-require.vue',
      'views/sc/sale/out/modify-un-require.vue',
    ].map(readSource);
    const disabledCallers = [
      'views/sc/purchase/order/add.vue',
      'views/sc/purchase/order/modify.vue',
      'views/sc/purchase/return/add-require.vue',
      'views/sc/purchase/return/add-un-require.vue',
      'views/sc/purchase/return/modify-require.vue',
      'views/sc/purchase/return/modify-un-require.vue',
      'views/sc/sale/order/add.vue',
      'views/sc/sale/order/modify.vue',
      'views/sc/sale/return/add-require.vue',
      'views/sc/sale/return/add-un-require.vue',
      'views/sc/sale/return/modify-require.vue',
      'views/sc/sale/return/modify-un-require.vue',
    ].map(readSource);

    expect(sharedBatchSource).toMatch(/showInquiryProduct:\s*\{[\s\S]*?default:\s*false/);
    expect(purchaseWrapper).toContain(':show-inquiry-product="showInquiryProduct"');
    expect(saleWrapper).toContain(':show-inquiry-product="showInquiryProduct"');
    expect(purchaseWrapper).toMatch(/showInquiryProduct:\s*\{[\s\S]*?default:\s*false/);
    expect(saleWrapper).toMatch(/showInquiryProduct:\s*\{[\s\S]*?default:\s*false/);
    enabledCallers.forEach((source) => {
      expect(source).toContain(':show-inquiry-product="true"');
    });
    disabledCallers.forEach((source) => {
      expect(source).not.toContain(':show-inquiry-product="true"');
    });
  });

  it('四个只读明细 API 将询价标识声明为可空布尔值', () => {
    const typeSources = [
      'api/sc/purchase/receive/model/getReceiveSheetBo.ts',
      'api/sc/purchase/receive/model/queryReceiveSheetDetailBo.ts',
      'api/sc/sale/out/model/getSaleOutSheetBo.ts',
      'api/sc/sale/out/model/querySaleOutSheetDetailBo.ts',
    ].map(readSource);

    typeSources.forEach((source) => {
      expect(source).toContain('inquiryProduct: boolean | null;');
    });
  });

  it('采购与销售来源订单明细 API 将询价标识声明为可空布尔值', () => {
    const typeSources = [
      'api/sc/purchase/order/model/purchaseOrderWithReceiveBo.ts',
      'api/sc/sale/order/model/saleOrderWithOutBo.ts',
    ].map(readSource);

    typeSources.forEach((source) => {
      expect(source).toContain('inquiryProduct: boolean | null;');
    });
  });

  it('共享格式化工具注释同时说明采购收货与销售出库用途', () => {
    const source = readSource('views/sc/components/inquiryProduct.ts');

    expect(source).toContain('采购收货与销售出库');
  });
});
