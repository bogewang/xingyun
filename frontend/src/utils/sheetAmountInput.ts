import { div, getNumber, mul } from '@/utils/utils';

type SheetAmountRow = Record<string, any>;

/**
 * 按数量和单价计算单行金额，并四舍五入到两位小数。
 */
export function calculateSheetLineAmount(
  quantity: string | number | null | undefined,
  price: string | number | null | undefined,
): number {
  return getNumber(mul(quantity || 0, price || 0), 2);
}

/**
 * 应用用户输入的手工金额，并在数量有效时反算单价。
 */
export function applyManualSheetAmount(
  row: SheetAmountRow,
  amount: string | number | null | undefined,
  quantityField: string,
  priceField: string,
): void {
  row.taxAmount = String(amount ?? '');
  row.lastValidTaxAmount = row.taxAmount;
  row.manualTaxAmount = true;

  const quantity = Number(row[quantityField]);
  // 数量非零且为有效数字时反算单价，支持负数
  if (quantity !== 0 && Number.isFinite(quantity) && row.taxAmount !== '') {
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
  row.taxAmount = calculateSheetLineAmount(row[quantityField], row[priceField]);
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

  const amount = calculateSheetLineAmount(row[quantityField], row[priceField]);
  if (row.lastValidTaxAmount === undefined) {
    const initialAmount = String(row.taxAmount ?? '');
    row.lastValidTaxAmount = initialAmount === '' ? String(amount) : initialAmount;
  }

  row.taxAmount = String(amount);

  return amount;
}
