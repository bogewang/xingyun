import { bignumber, type BigNumber } from 'mathjs';

type DecimalValue = string | number | null | undefined;

const DECIMAL_PATTERN = /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)$/;

export interface SaleOutStockRow {
  productId?: string | number | null;
  outNum?: string | number | null;
  stockNum?: string | number | null;
  baseStockNum?: string | number | null;
  conversionRate?: string | number | null;
  unitId?: string | number | null;
  units?: Array<{
    id?: string | number | null;
    conversionRate?: DecimalValue;
    [key: string]: unknown;
  }>;
}

function toSafeDecimal(value: DecimalValue): BigNumber | null {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? bignumber(value) : null;
  }

  if (typeof value !== 'string') {
    return null;
  }

  const normalizedValue = value.trim();
  return DECIMAL_PATTERN.test(normalizedValue) ? bignumber(normalizedValue) : null;
}

function getStockConversionRate(row: SaleOutStockRow): BigNumber {
  const rowRate = toSafeDecimal(row.conversionRate);
  if (rowRate?.gt(0)) {
    return rowRate;
  }

  const unitRate = toSafeDecimal(row.units?.find((unit) => unit.id === row.unitId)?.conversionRate);
  return unitRate?.gt(0) ? unitRate : bignumber(1);
}

function getBaseStockQuantity(row: SaleOutStockRow): BigNumber {
  const baseStockQuantity = toSafeDecimal(row.baseStockNum);
  if (baseStockQuantity) {
    return baseStockQuantity;
  }

  return (toSafeDecimal(row.stockNum) ?? bignumber(0)).mul(getStockConversionRate(row));
}

/**
 * 判断同一商品的累计出库数量是否未超过主单位库存。
 */
export function isSaleOutStockEnough(rows: SaleOutStockRow[], row: SaleOutStockRow): boolean {
  const totalOutNum = rows.reduce((total, item) => {
    if (item.productId !== row.productId) {
      return total;
    }

    const outNum = toSafeDecimal(item.outNum);
    if (!outNum?.gt(0)) {
      return total;
    }

    return total.add(outNum.mul(getStockConversionRate(item)));
  }, bignumber(0));

  return totalOutNum.lte(getBaseStockQuantity(row));
}
