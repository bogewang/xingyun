import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

describe('销售出库列表行操作', () => {
  it('将打印、修改、修改备注和删除收纳到更多菜单', () => {
    const source = readFileSync(new URL('../sheet-list.vue', import.meta.url), 'utf-8');
    const directActions = source.match(
      /createActions\(row\)\s*\{[\s\S]*?return\s*\[([\s\S]*?)\n\s*\];\n\s*\},/,
    )?.[1];
    const moreActions = source.match(
      /createMoreActions\(row\)\s*\{[\s\S]*?return\s*\[([\s\S]*?)\n\s*\];\n\s*\},/,
    )?.[1];

    expect(source).toContain(':drop-down-actions="createMoreActions(row)"');
    expect(source).toContain('更多<DownOutlined />');
    expect(directActions).not.toMatch(/label:\s*'(打印|修改|修改备注|删除)'/);
    expect(moreActions).toMatch(/label:\s*'打印'/);
    expect(moreActions).toMatch(/label:\s*'修改'/);
    expect(moreActions).toMatch(/label:\s*'修改备注'/);
    expect(moreActions).toMatch(/label:\s*'删除'/);
  });

  it('支持批量导出选中单据的明细', () => {
    const source = readFileSync(new URL('../sheet-list.vue', import.meta.url), 'utf-8');

    expect(source).toContain('@click="batchExportDetails"');
    expect(source).toMatch(/>批量导出明细<\/a-button\s*>/);
    expect(source).toMatch(/batchExportDetails\(\)\s*\{[\s\S]*?getCheckboxRecords\(\)/);
    expect(source).toMatch(
      /batchExportDetails\(\)\s*\{[\s\S]*?exportDetail\(\{[\s\S]*?idList:\s*records\.map\(\(item\)\s*=>\s*item\.id\)/,
    );
  });
});
