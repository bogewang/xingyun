import { beforeEach, describe, expect, it, vi } from 'vitest';

import * as printTemplateApi from '../index';

const httpGet = vi.hoisted(() => vi.fn());

vi.mock('/@/utils/http/axios', () => ({
  defHttp: {
    get: httpGet,
  },
}));

describe('打印模板字段说明接口', () => {
  beforeEach(() => {
    httpGet.mockResolvedValue([]);
    httpGet.mockClear();
  });

  it('按业务类型查询采购入库字段', async () => {
    await printTemplateApi.getFieldDesc(2);

    expect(httpGet).toHaveBeenCalledWith(
      {
        url: '/basedata/print/template/fieldDesc',
        params: { bizType: '2' },
      },
      { region: 'cloud-api' },
    );
  });

  it('未传业务类型时保持原请求结构', async () => {
    await printTemplateApi.getFieldDesc();

    expect(httpGet).toHaveBeenCalledWith(
      {
        url: '/basedata/print/template/fieldDesc',
      },
      { region: 'cloud-api' },
    );
  });
});
