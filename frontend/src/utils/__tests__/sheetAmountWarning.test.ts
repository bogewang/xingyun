import { describe, expect, it } from 'vitest';

import {
  getSheetAmountCellClass,
  getSheetTaxAmount,
  hasSheetAmountWarning,
} from '../sheetAmountWarning';

describe('单据金额预警', () => {
  const priceField = 'price';
  const quantityField = 'quantity';

  it('金额为空或输入非法时仅触发行预警', () => {
    expect(getSheetTaxAmount({ price: '', quantity: 1 }, priceField, quantityField)).toBeNull();
    expect(getSheetTaxAmount({ price: '-', quantity: 1 }, priceField, quantityField)).toBeNull();
    expect(hasSheetAmountWarning({ price: '', quantity: 1 }, priceField, quantityField)).toBe(true);
    expect(
      getSheetAmountCellClass({ price: '', quantity: 1 }, 'taxAmount', priceField, quantityField),
    ).toBe('');
  });

  it('数量或计算金额为零时标红对应字段', () => {
    const row = { price: 12.5, quantity: 0 };

    expect(getSheetTaxAmount(row, priceField, quantityField)).toBe(0);
    expect(hasSheetAmountWarning(row, priceField, quantityField)).toBe(true);
    expect(getSheetAmountCellClass(row, quantityField, priceField, quantityField)).toBe(
      'sheet-zero-warning-cell',
    );
    expect(getSheetAmountCellClass(row, priceField, priceField, quantityField)).toBe('');
    expect(getSheetAmountCellClass(row, 'taxAmount', priceField, quantityField)).toBe(
      'sheet-zero-warning-cell',
    );
  });

  it('单价为零时标红单价和金额字段', () => {
    const row = { price: 0, quantity: 2 };

    expect(getSheetAmountCellClass(row, priceField, priceField, quantityField)).toBe(
      'sheet-zero-warning-cell',
    );
    expect(getSheetAmountCellClass(row, 'taxAmount', priceField, quantityField)).toBe(
      'sheet-zero-warning-cell',
    );
  });

  it('按页面展示精度判断金额是否为零', () => {
    const row = { price: 0.001, quantity: 1 };

    expect(getSheetTaxAmount(row, priceField, quantityField)).toBe(0);
    expect(hasSheetAmountWarning(row, priceField, quantityField)).toBe(true);
  });
});
