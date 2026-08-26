import { beforeEach, describe, expect, it, vi } from 'vitest';

import * as printTemplateApi from '@/api/base-data/print-template';
import { getPrintTemplateSelection, vgBrowserPrint } from '../print';

vi.mock('@/api/base-data/print-template', () => ({
  query: vi.fn(),
  getSetting: vi.fn(),
}));

vi.mock('@/hooks/web/msg', () => ({
  createError: vi.fn(),
}));

describe('打印混入浏览器打印流程', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('加载业务模板并优先选中默认模板', async () => {
    vi.mocked(printTemplateApi.query).mockResolvedValue({
      datas: [
        { id: 'template-1', name: '模板一', bizType: '6' },
        { id: 'template-2', name: '模板二', bizType: '6', isDefault: true },
      ],
    } as any);

    const selection = await getPrintTemplateSelection(6);

    expect(printTemplateApi.query).toHaveBeenCalledWith(
      expect.objectContaining({ bizType: '6', pageIndex: 1, pageSize: 200 }),
    );
    expect(selection).toEqual({
      templateId: 'template-2',
      templateList: [
        { id: 'template-1', name: '模板一', bizType: '6' },
        { id: 'template-2', name: '模板二', bizType: '6' },
      ],
    });
  });

  it('使用选中模板配置调用浏览器打印运行时', async () => {
    const templateJson = { panels: [{ index: 0 }] };
    const printData = { id: 'sale-out-1' };
    const browserPrint = vi.fn();
    vi.mocked(printTemplateApi.getSetting).mockResolvedValue({ templateJson } as any);

    const options = { resetPageNumberPerData: true };
    await vgBrowserPrint.call(
      { $printRuntimeApi: { browserPrint } } as any,
      printData,
      'template-2',
      options,
    );

    expect(printTemplateApi.getSetting).toHaveBeenCalledWith('template-2');
    expect(browserPrint).toHaveBeenCalledWith(templateJson, printData, options);
  });
});
