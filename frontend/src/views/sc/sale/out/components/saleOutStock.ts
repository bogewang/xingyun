import { getUnitConversionRate } from '@/utils/productUnitConversion';
import { add, isFloatGtZero, mul } from '@/utils/utils';

export interface SaleOutStockRow {
  productId?: string | number | null;
  outNum?: string | number | null;
  stockNum?: string | number | null;
  baseStockNum?: string | number | null;
  conversionRate?: string | number | null;
  unitId?: string | number | null;
  units?: Array<Record<string, unknown>>;
}

function normalizeStockQuantity(value: string | number | null | undefined): number {
  const quantity = Number(value);
  return Number.isFinite(quantity) ? quantity : 0;
}

/**
 * 判断同一商品的累计出库数量是否未超过主单位库存。
 */
export function isSaleOutStockEnough(rows: SaleOutStockRow[], row: SaleOutStockRow): boolean {
  const checkArr = rows
    .filter((item) => item.productId === row.productId)
    .map((item) => mul(normalizeStockQuantity(item.outNum), getUnitConversionRate(item)));
  if (checkArr.length === 0) {
    checkArr.push(0);
  }
  const totalOutNum = checkArr.reduce((total, item) => {
    const outNum = isFloatGtZero(item) ? item : 0;
    return add(total, outNum);
  }, 0);

  return totalOutNum <= (row.baseStockNum ?? mul(row.stockNum || 0, row.conversionRate || 1));
}
