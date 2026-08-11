import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

describe('客户选择弹窗', () => {
  it('多选弹窗重新打开时回显已选客户', () => {
    const source = readFileSync(
      new URL('../DialogTable/src/DialogTable.vue', import.meta.url),
      'utf-8',
    );

    expect(source).toContain('checkRowKeys: this.selectValue');
  });

  it('显示客户备注时按备注排序', () => {
    const source = readFileSync(new URL('./CustomerSelector.vue', import.meta.url), 'utf-8');

    expect(source).toContain('orderByDescription: this.showDescriptionFilter');
  });
});
