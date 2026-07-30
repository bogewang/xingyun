import { describe, expect, it, vi } from 'vitest';
import { readFileSync } from 'node:fs';
import { stopGridDeleteFromInput } from '../productAddShortcut';

describe('表格输入框删除键处理', () => {
  it('小键盘小数点作为首个输入时不会冒泡到表格', () => {
    const event = {
      key: '.',
      code: 'NumpadDecimal',
      keyCode: 110,
      stopPropagation: vi.fn(),
    } as unknown as KeyboardEvent;

    stopGridDeleteFromInput(event);

    expect(event.stopPropagation).toHaveBeenCalledOnce();
  });

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

  it('销售出库数量输入框绑定删除键拦截处理', () => {
    const source = readFileSync(
      new URL('../../views/sc/sale/out/add-un-require.vue', import.meta.url),
      'utf-8',
    );

    expect(source).toContain('@keydown="stopGridDeleteFromInput"');
  });
});
