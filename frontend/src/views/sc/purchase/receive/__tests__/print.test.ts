import { describe, expect, it, vi } from 'vitest';

import { PRINT_TYPE } from '@/enums/biz/printType';
import { browserPrintReceiveSheet } from '../print';

describe('采购入库打印流程', () => {
  it('加载采购入库打印数据后使用采购入库业务类型浏览器打印', async () => {
    const printData = {
      id: 'receive-1',
    } as any;
    const loadPrintData = vi.fn().mockResolvedValue(printData);
    const browserPrint = vi.fn().mockResolvedValue(undefined);

    await browserPrintReceiveSheet('receive-1', loadPrintData, browserPrint);

    expect(loadPrintData).toHaveBeenCalledWith('receive-1');
    expect(browserPrint).toHaveBeenCalledWith(PRINT_TYPE.RECEIVE_SHEET.code, printData);
  });
});
