import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

const source = readFileSync(new URL('../sheet-list.vue', import.meta.url), 'utf-8');

/** 执行列表实际格式化方法，验证前端仅展示后端毛利率。 */
function createListContext() {
  const body = source.match(/formatProfitRate\(value\) \{([\s\S]*?)\n {6}\},/)![1];
  return {
    canViewProfit: true,
    summaryProfitRate: 7.14,
    formatProfitRate: new Function('value', body),
    /** 汇总测试单据的指定金额字段。 */
    sumByField(data, field) {
      return data.reduce((sum, row) => sum + Number(row[field] || 0), 0);
    },
    /** 格式化测试金额。 */
    formatAmount(value) {
      return Number(value).toFixed(2);
    },
  };
}

describe('销售出库查询毛利率展示', () => {
  it('行毛利率直接展示后端返回值', () => {
    const expression = source.match(/#profit_rate="\{ row \}"[\s\S]*?\{\{\s*(.*?)\s*\}\}/)![1];
    const render = new Function('row', 'formatProfitRate', `return ${expression}`);
    expect(render({ profitRate: 6.01 }, createListContext().formatProfitRate)).toBe('6.01%');
  });

  it('合计毛利率展示后端结果，不根据本地行数据重新计算', () => {
    const body = source.match(/footerMethod\(\{ columns, data \}\) \{([\s\S]*?)\n {6}\},/)![1];
    const footer = new Function('columns', 'data', body);
    expect(footer.call(createListContext(), [{ field: 'profitRate' }], [])).toEqual([['7.14%']]);
  });

  it('零值、负值及缺失值展示正确', () => {
    const { formatProfitRate } = createListContext();
    expect(formatProfitRate(0)).toBe('0.00%');
    expect(formatProfitRate(-12.89)).toBe('-12.89%');
    expect(formatProfitRate(null)).toBe('-');
  });
});
