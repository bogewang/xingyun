import { getNumber, isFloatGeZero, mul } from '@/utils/utils';

type SheetRow = Record<string, unknown>;

export function getSheetTaxAmount(
  row: SheetRow,
  priceField: string,
  quantityField: string,
): number | null {
  const price = row[priceField];
  const quantity = row[quantityField];
  if (!isFloatGeZero(price) || !isFloatGeZero(quantity)) {
    return null;
  }

  return getNumber(mul(price, quantity), 2);
}

export function hasSheetAmountWarning(row: SheetRow, priceField: string, quantityField: string) {
  const taxAmount = getSheetTaxAmount(row, priceField, quantityField);
  return taxAmount === null || Number(taxAmount) === 0;
}

export function getSheetAmountCellClass(
  row: SheetRow,
  field: string,
  priceField: string,
  quantityField: string,
) {
  const quantity = row[quantityField];
  const price = row[priceField];
  const taxAmount = getSheetTaxAmount(row, priceField, quantityField);
  if (field === priceField && isFloatGeZero(price) && Number(price) === 0) {
    return 'sheet-zero-warning-cell';
  }

  if (field === quantityField && isFloatGeZero(quantity) && Number(quantity) === 0) {
    return 'sheet-zero-warning-cell';
  }

  if (field === 'taxAmount' && taxAmount !== null && Number(taxAmount) === 0) {
    return 'sheet-zero-warning-cell';
  }

  return '';
}
