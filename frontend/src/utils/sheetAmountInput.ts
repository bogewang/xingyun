import { div, getNumber, mul } from '@/utils/utils';

import { sanitizeNonNegativeDecimalInput } from './numberInput';

type SheetAmountRow = Record<string, any>;

/**
 * 应用用户输入的手工金额，并在数量有效时反算单价。
 */
export function applyManualSheetAmount(
  row: SheetAmountRow,
  amount: string | number | null | undefined,
  quantityField: string,
  priceField: string,
): void {
  const autoAmount = getNumber(mul(row[quantityField] || 0, row[priceField] || 0), 2);
  const currentAmount = String(row.taxAmount ?? '');
  const initialAmount = sanitizeNonNegativeDecimalInput(currentAmount);
  const previousAmount = String(
    row.lastValidTaxAmount ?? (initialAmount === '' && currentAmount !== '' ? autoAmount : currentAmount),
  );
  row.taxAmount = sanitizeNonNegativeDecimalInput(amount, previousAmount);
  row.lastValidTaxAmount = row.taxAmount;
  row.manualTaxAmount = true;

  const quantity = Number(row[quantityField]);
  if (quantity > 0 && row.taxAmount !== '') {
    row[priceField] = getNumber(div(row.taxAmount, quantity), 6);
  }
}

/**
 * 清除明细行的手工金额标识，恢复自动金额计算。
 */
export function clearManualSheetAmount(
  row: SheetAmountRow,
  quantityField: string,
  priceField: string,
): void {
  delete row.manualTaxAmount;
  row.taxAmount = getNumber(mul(row[quantityField] || 0, row[priceField] || 0), 2);
  row.lastValidTaxAmount = String(row.taxAmount);
}

/**
 * 获取明细行金额；手工金额优先，否则使用数量乘单价并保留两位小数。
 */
export function getSheetLineAmount(
  row: SheetAmountRow,
  quantityField: string,
  priceField: string,
): number {
  if (row.manualTaxAmount) {
    return Number(row.taxAmount || 0);
  }

  if (row.taxAmount !== '' && row.taxAmount !== null && row.taxAmount !== undefined) {
    const storedAmount = Number(row.taxAmount);
    if (Number.isFinite(storedAmount)) {
      return storedAmount;
    }
  }

  const amount = getNumber(mul(row[quantityField] || 0, row[priceField] || 0), 2);
  if (row.lastValidTaxAmount === undefined) {
    const initialAmount = sanitizeNonNegativeDecimalInput(row.taxAmount, String(amount));
    row.lastValidTaxAmount = initialAmount === '' ? String(amount) : initialAmount;
  }

  row.taxAmount = String(amount);

  return amount;
}
