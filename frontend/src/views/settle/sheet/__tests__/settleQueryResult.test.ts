import { describe, expect, it } from 'vitest';

import { normalizeSettleQueryResult } from '../settleQueryResult';

describe('结算工作台分页查询结果', () => {
  it('返回分页响应中的数据数组与总条数', () => {
    expect(
      normalizeSettleQueryResult({
        datas: [{ id: 'receive-sheet-1' }],
        totalCount: 12,
      }),
    ).toEqual({
      datas: [{ id: 'receive-sheet-1' }],
      totalCount: 12,
    });
  });

  it('空响应或空数据安全地返回空数组和零总数', () => {
    expect(normalizeSettleQueryResult()).toEqual({ datas: [], totalCount: 0 });
    expect(normalizeSettleQueryResult({ datas: null, totalCount: undefined })).toEqual({
      datas: [],
      totalCount: 0,
    });
  });
});
