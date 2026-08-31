import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { buildQuoteSheetPayload, resolveQuoteProductUnitName } from './quoteSheet';

describe('报价单编辑数据', () => {
  it('列表页查看报价单时应打开详情弹窗', () => {
    const source = readFileSync(new URL('./index.vue', import.meta.url), 'utf-8');
    const detailSource = readFileSync(new URL('./detail.vue', import.meta.url), 'utf-8');

    expect(source).toContain('<detail :id="id" ref="viewDialog" />');
    expect(source).toContain('this.$refs.viewDialog.openDialog()');
    expect(detailSource).toContain('<a-modal');
    expect(detailSource).toContain('openDialog()');
  });

  it('详情商品应将单位 ID 转为单位名称', () => {
    expect(resolveQuoteProductUnitName('unit-1', { 'unit-1': '箱' })).toBe('箱');
    expect(resolveQuoteProductUnitName('历史单位', { 'unit-1': '箱' })).toBe('历史单位');
  });

  it('查看弹窗的报价商品表格应固定在弹窗剩余高度内滚动', () => {
    const source = readFileSync(new URL('./detail.vue', import.meta.url), 'utf-8');

    expect(source).toContain('class="quote-sheet-detail-grid-border"');
    expect(source).toContain('class="quote-sheet-detail-grid-container"');
    expect(source).toContain('.quote-sheet-detail-grid-border {\n    height: 100%;');
    expect(source).toContain('.quote-sheet-detail-grid-container {\n    flex: 1;');
  });

  it('查询页和查看页均提供报价单商品明细导出入口', () => {
    const listSource = readFileSync(new URL('./index.vue', import.meta.url), 'utf-8');
    const detailSource = readFileSync(new URL('./detail.vue', import.meta.url), 'utf-8');

    expect(listSource).toContain("label: '导出明细'");
    expect(listSource).toContain('api.exportDetail({ idList: [id] })');
    expect(detailSource).toContain('@click="exportDetails"');
    expect(detailSource).toMatch(/api\s*\.exportDetail\(\{\s*idList: \[this\.id\]\s*\}\)/);
  });

  it('列表页首次查询默认筛选启用状态', () => {
    const source = readFileSync(new URL('./index.vue', import.meta.url), 'utf-8');

    expect(source).toContain("searchForm: { name: '', status: 'ENABLED' }");
  });

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

  it('编辑页支持批量添加商品，并仅传递报价单 ID 由后端排除已有明细', () => {
    const source = readFileSync(new URL('./modify.vue', import.meta.url), 'utf-8');
    const batchSource = readFileSync(
      new URL('../../sc/shared/batch-add-product.vue', import.meta.url),
      'utf-8',
    );

    expect(source).toContain('openBatchAddProductDialog');
    expect(source).toContain(':quote-sheet-id="formData.id"');
    expect(source).toContain('batchAddProduct(productList)');
    expect(batchSource).toContain("this.bizType === 'quote'");
    expect(batchSource).toContain('quoteSheetId');
    expect(batchSource).not.toContain('excludeProductIds');
    expect(batchSource).toContain('loadQuoteUnitNames');
    expect(batchSource).toContain('quoteUnitNameMap[product.unit] || product.unit');
    expect(batchSource).not.toContain("{ field: 'brandName', title: '商品品牌'");
    expect(batchSource).toContain('v-if="bizType !== \'quote\'" label="商品品牌"');
    expect(batchSource).toContain("? { quoteSheetId: this.quoteSheetId }");
  });

  it('编辑页保存或关闭时必须通过多标签页机制关闭当前标签', () => {
    const source = readFileSync(new URL('./modify.vue', import.meta.url), 'utf-8');

    expect(source).toContain("import { multiplePageMix } from '@/mixins/multiplePageMix';");
    expect(source).toContain('mixins: [multiplePageMix]');
    expect(source).toContain('closeDialog() {\n        this.closeCurrentPage();');
  });

  it('新增和编辑页使用日期范围组件并映射开始、结束日期', () => {
    for (const file of ['./add.vue', './modify.vue']) {
      const source = readFileSync(new URL(file, import.meta.url), 'utf-8');
      expect(source).toContain('<a-range-picker');
      expect(source).toContain('v-model:value="dateRange"');
      expect(source).toContain('this.formData.startDate = value?.[0] ||');
      expect(source).toContain('this.formData.endDate = value?.[1] ||');
    }
  });

  it('新增和编辑页的明细表格应只占用表单和操作区之外的剩余高度', () => {
    for (const file of ['./add.vue', './modify.vue']) {
      const source = readFileSync(new URL(file, import.meta.url), 'utf-8');

      expect(source).toContain('<div class="sheet-editor-grid-wrapper">');
      expect(source).toContain('height="100%"');
      expect(source).not.toContain('height="auto"');
      expect(source).toContain(':scroll-y="{ enabled: false }"');
      expect(source).toContain('@wheel.capture.stop');
      expect(source).toContain('.sheet-editor-grid-wrapper {\n    flex: 1;');
      expect(source).toContain('.sheet-editor-grid {\n    height: 100%;');
      expect(source).toContain('min-height: 0;');
    }
  });

  it('报价单商品下拉不展示 SKU，并将单位 ID 转为单位名称', () => {
    const selectorSource = readFileSync(
      new URL('../../sc/shared/inline-product-select.vue', import.meta.url),
      'utf-8',
    );

    expect(selectorSource).not.toContain('v-if="isQuote" field="skuCode"');
    expect(selectorSource).toContain('getUnitName(product.unit)');
    for (const file of ['./add.vue', './modify.vue']) {
      const pageSource = readFileSync(new URL(file, import.meta.url), 'utf-8');
      expect(pageSource).toContain(':unit-name-map="unitNameMap"');
    }
  });
});
