import { describe, expect, it } from 'vitest';
import { getConfirmAmount, sumConfirmFields } from '../saleOutConfirm';

describe('saleOutConfirm', () => {
  it('calculates amount from confirm quantity and tax price', () => {
    expect(getConfirmAmount({ confirmNum: 1.234567, taxPrice: 2.345678 })).toBe(2.895897);
  });

  it('sums detail acceptance fields and treats empty values as zero', () => {
    expect(
      sumConfirmFields([
        { confirmNum: 2, confirmAmt: 6.5 },
        { confirmNum: null, confirmAmt: null },
      ]),
    ).toEqual({ confirmNum: 2, confirmAmt: 6.5 });
  });
});
