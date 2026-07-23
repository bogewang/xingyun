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
});
