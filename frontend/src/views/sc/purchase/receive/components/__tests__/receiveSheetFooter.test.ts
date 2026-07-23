import { describe, expect, it } from 'vitest';

import { buildReceiveSheetFooter } from '../receiveSheetFooter';

describe('采购收货单合计行', () => {
  it('汇总商品数量、总金额、已付金额和未付金额', () => {
    expect(
      buildReceiveSheetFooter(
        [
          { type: 'seq' },
          { field: 'totalNum' },
          { field: 'totalAmount' },
          { field: 'paidAmount' },
          { field: 'unpaidAmount' },
          { field: 'supplierName' },
        ],
        [
          { totalNum: 2.5, totalAmount: 100, paidAmount: 40, unpaidAmount: 60 },
          { totalNum: 1, totalAmount: 20.5, paidAmount: 5.25, unpaidAmount: 15.25 },
        ],
      ),
    ).toEqual([['合计', '3.5', '120.50', '45.25', '75.25', '']]);
  });

  it('将缺失和非有限或非数值字段按零处理且不抛出异常', () => {
    expect(buildReceiveSheetFooter([{ type: 'seq' }, { field: 'totalAmount' }], [])).toEqual([
      ['合计', '0.00'],
    ]);

    expect(() =>
      buildReceiveSheetFooter(
        [
          { type: 'seq' },
          { field: 'totalNum' },
          { field: 'totalAmount' },
          { field: 'paidAmount' },
          { field: 'unpaidAmount' },
        ],
        [
          {},
          {
            totalNum: null,
            totalAmount: 'invalid',
            paidAmount: Infinity,
            unpaidAmount: Symbol('unpaidAmount'),
          },
        ],
      ),
    ).not.toThrow();

    expect(
      buildReceiveSheetFooter(
        [
          { type: 'seq' },
          { field: 'totalNum' },
          { field: 'totalAmount' },
          { field: 'paidAmount' },
          { field: 'unpaidAmount' },
        ],
        [
          {},
          {
            totalNum: null,
            totalAmount: 'invalid',
            paidAmount: Infinity,
            unpaidAmount: Symbol('unpaidAmount'),
          },
        ],
      ),
    ).toEqual([['合计', '0', '0.00', '0.00', '0.00']]);
  });

  it('在有限数累加溢出时回退为零并保留数字字符串金额兼容', () => {
    expect(
      buildReceiveSheetFooter(
        [{ type: 'seq' }, { field: 'totalAmount' }, { field: 'paidAmount' }],
        [
          { totalAmount: Number.MAX_VALUE, paidAmount: '40.25' },
          { totalAmount: Number.MAX_VALUE, paidAmount: 5 },
        ],
      ),
    ).toEqual([['合计', '0.00', '45.25']]);
  });

  it('已结算单据将结算金额并入已付并从对账金额扣减未付', () => {
    expect(
      buildReceiveSheetFooter(
        [{ type: 'seq' }, { field: 'paidAmount' }, { field: 'unpaidAmount' }],
        [{ settleStatus: 3, checkAmount: 100, settleAmount: 60, paidAmount: 10, unpaidAmount: 90 }],
      ),
    ).toEqual([['合计', '70.00', '30.00']]);
  });

  it('非已结算单据继续使用原始已付和未付金额', () => {
    expect(
      buildReceiveSheetFooter(
        [{ type: 'seq' }, { field: 'paidAmount' }, { field: 'unpaidAmount' }],
        [{ settleStatus: 1, checkAmount: 100, settleAmount: 60, paidAmount: 10, unpaidAmount: 90 }],
      ),
    ).toEqual([['合计', '10.00', '90.00']]);
  });
});
