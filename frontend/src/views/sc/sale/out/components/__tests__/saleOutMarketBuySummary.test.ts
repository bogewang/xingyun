import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';

import {
  buildMarketBuySummary2Params,
  buildMarketBuySummaryParams,
} from '../saleOutMarketBuySummary';

describe('买菜汇总请求参数', () => {
  it('买菜汇总导出按二进制响应处理', () => {
    const apiSource = readFileSync(
      new URL('../../../../../../api/sc/sale/out/index.ts', import.meta.url),
      'utf-8',
    );

    expect(apiSource).toMatch(
      /url: baseUrl \+ '\/export\/marketBuySummary',\s*data: params,\s*},\s*{\s*region,\s*contentType: ContentTypeEnum\.JSON,\s*responseType: ResponseEnum\.BLOB/,
    );
  });

  it('买菜汇总2导出按二进制响应处理', () => {
    const apiSource = readFileSync(
      new URL('../../../../../../api/sc/sale/out/index.ts', import.meta.url),
      'utf-8',
    );

    expect(apiSource).toMatch(
      /url: baseUrl \+ '\/export\/marketBuySummary2',\s*data: params,\s*},\s*{\s*region,\s*contentType: ContentTypeEnum\.JSON,\s*responseType: ResponseEnum\.BLOB/,
    );
  });

  it('买菜汇总默认不按日期汇总', () => {
    expect(
      buildMarketBuySummaryParams([
        { id: 'sheet-1', code: 'SO-001' },
        { id: 'sheet-2', code: 'SO-002' },
      ]),
    ).toEqual({
      idList: ['sheet-1', 'sheet-2'],
      groupByDate: false,
      mergeSameDayCustomerProduct: false,
    });
  });

  it('买菜汇总勾选后按日期汇总', () => {
    expect(buildMarketBuySummaryParams([{ id: 'sheet-1' }], true)).toEqual({
      idList: ['sheet-1'],
      groupByDate: true,
      mergeSameDayCustomerProduct: false,
    });
  });

  it('买菜汇总勾选后传递同日同客户商品合并选项', () => {
    expect(buildMarketBuySummaryParams([{ id: 'sheet-1' }], false, true)).toEqual({
      idList: ['sheet-1'],
      groupByDate: false,
      mergeSameDayCustomerProduct: true,
    });
  });

  it('买菜汇总2根据勾选单据构建单据ID筛选条件', () => {
    expect(
      buildMarketBuySummary2Params([{ id: 'sheet-1' }, { id: undefined }, { id: 'sheet-2' }]),
    ).toEqual({ idList: ['sheet-1', 'sheet-2'] });
  });
});
