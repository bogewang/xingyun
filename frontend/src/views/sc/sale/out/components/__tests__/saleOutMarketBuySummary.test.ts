import { describe, expect, it } from 'vitest';

import {
  buildMarketBuySummary2Params,
  buildMarketBuySummaryParams,
} from '../saleOutMarketBuySummary';

describe('买菜汇总请求参数', () => {
  it('根据勾选单据构建单据ID筛选条件', () => {
    expect(
      buildMarketBuySummaryParams([
        { id: 'sheet-1', code: 'SO-001' },
        { id: 'sheet-2', code: 'SO-002' },
      ]),
    ).toEqual({
      idList: ['sheet-1', 'sheet-2'],
    });
  });

  it('买菜汇总2根据勾选单据构建单据ID筛选条件', () => {
    expect(buildMarketBuySummary2Params([{ id: 'sheet-1' }, { id: undefined }, { id: 'sheet-2' }]))
      .toEqual({ idList: ['sheet-1', 'sheet-2'] });
  });
});
