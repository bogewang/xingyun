import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

/**
 * 读取行内商品选择组件源代码。
 */
function readInlineProductSelectSource() {
  return readFileSync(resolve(__dirname, '..', 'inline-product-select.vue'), 'utf-8');
}

describe('采购入库和销售出库商品选择备注展示', () => {
  it('商品选择表格的最后一列为备注', () => {
    const source = readInlineProductSelectSource();
    const tableEndIndex = source.indexOf('</vxe-table>');
    const lastColumnIndex = source.lastIndexOf('<vxe-column', tableEndIndex);
    const lastColumn = source.slice(lastColumnIndex, tableEndIndex);

    expect(lastColumn).toContain('field="remark"');
    expect(lastColumn).toContain('title="备注"');
  });

  it('新增和修改表格将商品备注与录单备注分列展示', () => {
    const sourceRoot = resolve(__dirname, '..', '..');
    const pagePaths = [
      'purchase/receive/add-require.vue',
      'purchase/receive/add-un-require.vue',
      'purchase/receive/modify-require.vue',
      'purchase/receive/modify-un-require.vue',
      'sale/out/add-require.vue',
      'sale/out/add-un-require.vue',
      'sale/out/modify-require.vue',
      'sale/out/modify-un-require.vue',
    ];

    pagePaths.forEach((pagePath) => {
      const source = readFileSync(resolve(sourceRoot, pagePath), 'utf-8');

      expect(source).toContain("field: 'productRemark'");
      expect(source).toContain("title: '商品备注'");
      expect(source).toContain("field: 'description'");
      expect(source).toContain("title: '备注'");
      expect(source).toContain('productRemark: product.remark');
    });
  });
});
