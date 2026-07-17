import { describe, expect, it, vi } from 'vitest';

import { PRINT_TYPE } from '@/enums/biz/printType';
import { previewReceiveSheetPrint } from '../print';

describe('采购入库打印流程', () => {
  it('加载采购入库打印数据后使用采购入库业务类型预览', async () => {
    const printData = {
      id: 'receive-1',
    } as any;
    const loadPrintData = vi.fn().mockResolvedValue(printData);
    const preview = vi.fn().mockResolvedValue(undefined);

    await previewReceiveSheetPrint('receive-1', loadPrintData, preview);

    expect(loadPrintData).toHaveBeenCalledWith('receive-1');
    expect(preview).toHaveBeenCalledWith(PRINT_TYPE.RECEIVE_SHEET.code, printData);
  });
});
