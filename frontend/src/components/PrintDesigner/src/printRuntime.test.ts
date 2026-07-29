import { describe, expect, it, vi } from 'vitest';
import printRuntime from './printRuntime';

const { openPrintDialog } = vi.hoisted(() => ({ openPrintDialog: vi.fn() }));

vi.mock('/@/components/PrintDialog', () => ({ openPrintDialog }));

describe('printRuntime', () => {
  it('原样将模板和业务数据交给运行时弹窗，由弹窗判断旧模板', () => {
    const legacyTemplate = { panels: [] };
    const printData = { orderNo: 'SO-001' };

    printRuntime.preview(legacyTemplate, printData);

    expect(openPrintDialog).toHaveBeenCalledWith(
      expect.objectContaining({
        templateJson: legacyTemplate,
        printData,
      }),
    );
  });
});
