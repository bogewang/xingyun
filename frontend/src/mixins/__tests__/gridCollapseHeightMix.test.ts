import { describe, expect, it, vi } from 'vitest';
import { gridCollapseHeightMix } from '../gridCollapseHeightMix';

describe('gridCollapseHeightMix', () => {
  it('组件首次挂载时同步表格高度，避免分页栏被自动高度挤出视口', () => {
    const handleGridResize = vi.fn();
    const syncGridHeight = vi.fn();
    const addEventListener = vi.fn();
    const setTimeout = vi.fn();
    const originalWindow = globalThis.window;

    Object.defineProperty(globalThis, 'window', {
      configurable: true,
      value: { addEventListener, setTimeout },
    });

    try {
      gridCollapseHeightMix.mounted.call({ handleGridResize, syncGridHeight });

      expect(addEventListener).toHaveBeenCalledWith('resize', handleGridResize);
      expect(syncGridHeight).toHaveBeenCalledOnce();
      expect(setTimeout).toHaveBeenCalledWith(expect.any(Function), 240);
    } finally {
      Object.defineProperty(globalThis, 'window', {
        configurable: true,
        value: originalWindow,
      });
    }
  });
});
