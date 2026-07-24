import { describe, expect, it } from 'vitest';
import {
  getConfirmAmount,
  normalizeConfirmNum,
  sumConfirmFields,
  syncConfirmAmount,
} from '../saleOutConfirm';

describe('saleOutConfirm', () => {
  it('calculates amount from confirm quantity and tax price', () => {
    expect(getConfirmAmount({ confirmNum: 1.234567, taxPrice: 2.345678 })).toBe(2.895897);
  });

  it('normalizes confirm quantity to number for payloads', () => {
    expect(normalizeConfirmNum('5.1')).toBe(5.1);
    expect(typeof normalizeConfirmNum('5.1')).toBe('number');
    expect(normalizeConfirmNum(null)).toBe(0);
  });

  it('returns zero when confirm quantity or tax price is empty', () => {
    expect(getConfirmAmount({ confirmNum: null, taxPrice: 2.345678 })).toBe(0);
    expect(getConfirmAmount({ confirmNum: 1.234567, taxPrice: null })).toBe(0);
    expect(getConfirmAmount({ confirmNum: null, taxPrice: null })).toBe(0);
  });

  it('syncs confirm amount back to the row while keeping confirm fields unchanged', () => {
    const row = { confirmNum: 1.25, taxPrice: 3.2, confirmAmt: null };

    expect(syncConfirmAmount(row)).toBe(4);
    expect(row.confirmAmt).toBe(4);
    expect(row.confirmNum).toBe(1.25);
    expect(row.taxPrice).toBe(3.2);
  });

  it('sums detail acceptance fields and treats empty values as zero', () => {
    expect(
      sumConfirmFields([
        { confirmNum: 2, confirmAmt: 6.5 },
        { confirmNum: null, confirmAmt: null },
      ]),
    ).toEqual({ confirmNum: 2, confirmAmt: 6.5 });
  });

  it('handles empty list and multi-row boundaries when summing acceptance fields', () => {
    expect(sumConfirmFields([])).toEqual({ confirmNum: 0, confirmAmt: 0 });
    expect(
      sumConfirmFields([
        { confirmNum: 0, confirmAmt: 0 },
        { confirmNum: 1.234567, confirmAmt: 2.345678 },
        { confirmNum: null, confirmAmt: undefined },
      ]),
    ).toEqual({ confirmNum: 1.234567, confirmAmt: 2.345678 });
  });
});
