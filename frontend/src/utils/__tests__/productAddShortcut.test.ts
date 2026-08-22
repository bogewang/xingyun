import { describe, expect, it, vi } from 'vitest';
import { readFileSync } from 'node:fs';
import { stopGridDeleteFromInput } from '../productAddShortcut';

describe('表格输入框快捷键处理', () => {
  it('小键盘 Del 在数量输入框中不会冒泡到表格', () => {
    const event = {
      key: 'Delete',
      stopPropagation: vi.fn(),
    } as unknown as KeyboardEvent;

    stopGridDeleteFromInput(event);

    expect(event.stopPropagation).toHaveBeenCalledOnce();
  });

  it('普通按键仍可继续冒泡', () => {
    const event = {
      key: 'Enter',
      stopPropagation: vi.fn(),
    } as unknown as KeyboardEvent;

    stopGridDeleteFromInput(event);

    expect(event.stopPropagation).not.toHaveBeenCalled();
  });

  it('空格键在数量输入框中不会冒泡到表格', () => {
    const event = {
      key: ' ',
      preventDefault: vi.fn(),
      stopPropagation: vi.fn(),
    } as unknown as KeyboardEvent;

    stopGridDeleteFromInput(event);

    expect(event.preventDefault).toHaveBeenCalledOnce();
    expect(event.stopPropagation).toHaveBeenCalledOnce();
  });

  it('销售出库新增数量输入框绑定统一键盘处理', () => {
    const source = readFileSync(
      new URL('../../views/sc/sale/out/add-un-require.vue', import.meta.url),
      'utf-8',
    );

    expect(source).toContain(
      '@keydown="(e) => handleTableInputKeyDown(e, \'outNumInputRef\', rowIndex, true)"',
    );
    expect(source).toContain('stopGridDeleteFromInput(event);');
  });

  it('销售出库修改数量输入框绑定统一键盘处理', () => {
    const source = readFileSync(
      new URL('../../views/sc/sale/out/modify-un-require.vue', import.meta.url),
      'utf-8',
    );

    expect(source).toContain(
      '@keydown="(e) => handleTableInputKeyDown(e, \'outNumInputRef\', rowIndex, true)"',
    );
    expect(source).toContain('stopGridDeleteFromInput(event);');
  });

  it('销售出库订单新增页绑定上键切换处理', () => {
    const source = readFileSync(
      new URL('../../views/sc/sale/out/add-require.vue', import.meta.url),
      'utf-8',
    );

    expect(source).toContain(
      '@keydown="(e) => handleTableInputKeyDown(e, \'outNumInputRef\', rowIndex)"',
    );
    expect(source).toContain("event.key === 'ArrowUp'");
    expect(source).toContain('event.stopPropagation();');
  });

  it('销售出库非订单修改页绑定上键切换处理', () => {
    const source = readFileSync(
      new URL('../../views/sc/sale/out/modify-un-require.vue', import.meta.url),
      'utf-8',
    );

    expect(source).toContain("event.key === 'ArrowUp'");
    expect(source).toContain('event.stopPropagation();');
  });

  it('销售出库订单修改页绑定上键切换处理', () => {
    const source = readFileSync(
      new URL('../../views/sc/sale/out/modify-require.vue', import.meta.url),
      'utf-8',
    );

    expect(source).toContain(
      '@keydown="(e) => handleTableInputKeyDown(e, \'outNumInputRef\', rowIndex)"',
    );
    expect(source).toContain("event.key === 'ArrowUp'");
    expect(source).toContain('event.stopPropagation();');
  });
});
