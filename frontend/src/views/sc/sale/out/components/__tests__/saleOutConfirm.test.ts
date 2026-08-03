import { describe, expect, it } from 'vitest';
import { calculateSheetLineAmount } from '@/utils/sheetAmountInput';
import {
  formatConfirmAmount,
  getConfirmAmount,
  normalizeConfirmNum,
  sumConfirmFields,
  syncConfirmAmount,
} from '../saleOutConfirm';

describe('saleOutConfirm', () => {
  it('calculates amount from confirm quantity and tax price', () => {
    expect(getConfirmAmount({ confirmNum: 1.234567, taxPrice: 2.345678 })).toBe(2.9);
  });

  it('uses the same line amount rule when quantities are equal', () => {
    const quantity = 1.004;
    const taxPrice = 1;

    expect(getConfirmAmount({ confirmNum: quantity, taxPrice })).toBe(
      calculateSheetLineAmount(quantity, taxPrice),
    );
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

  it('rounds each acceptance amount to two decimals before summing', () => {
    const rows = [
      { confirmNum: 1.004, taxPrice: 1, confirmAmt: null },
      { confirmNum: 1.004, taxPrice: 1, confirmAmt: null },
    ];

    rows.forEach((row) => syncConfirmAmount(row));

    expect(rows.map((row) => row.confirmAmt)).toEqual([1, 1]);
    expect(sumConfirmFields(rows).confirmAmt).toBe(2);
  });

  it('formats acceptance amount with two decimal places for display', () => {
    expect(formatConfirmAmount(2.895897)).toBe('2.90');
    expect(formatConfirmAmount(4)).toBe('4.00');
    expect(formatConfirmAmount(null)).toBe('0.00');
    expect(formatConfirmAmount('invalid')).toBe('0.00');
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
