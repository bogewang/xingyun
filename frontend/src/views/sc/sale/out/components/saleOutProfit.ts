import { getNumber, isFloatGeZero, mul } from '@/utils/utils';

export interface SaleOutProfitRow {
  taxPrice?: number | string | null;
  taxAmount?: number | string | null;
  outNum?: number | string | null;
  confirmAmt?: number | string | null;
  costPrice?: number | string | null;
}

/**
 * 获取销售出库明细行出库金额，优先使用金额列，缺失时按价格乘数量计算。
 * @param row 销售出库明细行
 * @returns 出库金额
 */
export function getSaleOutAmount(row: SaleOutProfitRow): number {
  const taxAmount = Number(row?.taxAmount);
  if (Number.isFinite(taxAmount)) {
    return taxAmount;
  }

  return Number(getNumber(mul(row?.taxPrice || 0, row?.outNum || 0), 2));
}

/**
 * 计算销售出库明细利润额，利润按验收金额减出库成本。
 * @param row 销售出库明细行
 * @returns 利润额
 */
export function calcSaleOutProfitAmount(row: SaleOutProfitRow): number {
  const amt = getSaleOutAmount(row);
  const costAmount = Number(getNumber(mul(row?.costPrice || 0, row?.outNum || 0), 2));
  return amt - costAmount;
}

/**
 * 计算销售出库明细毛利率，利润按验收金额减出库成本，分母按出库金额。
 * @param row 销售出库明细行
 * @returns 毛利率文本
 */
export function calcSaleOutProfitRate(row: SaleOutProfitRow): string {
  const outAmount = getSaleOutAmount(row);
  if (!outAmount || !isFloatGeZero(row?.costPrice) || !isFloatGeZero(row?.outNum)) {
    return '0.00%';
  }

  return `${((calcSaleOutProfitAmount(row) / outAmount) * 100).toFixed(2)}%`;
}

/**
 * 判断销售出库明细是否为负毛利。
 * @param row 销售出库明细行
 * @returns 是否负毛利
 */
export function isSaleOutProfitNegative(row: SaleOutProfitRow): boolean {
  if (
    !isFloatGeZero(row?.taxPrice) ||
    !isFloatGeZero(row?.costPrice) ||
    !isFloatGeZero(row?.outNum)
  ) {
    return false;
  }

  return calcSaleOutProfitAmount(row) < 0;
}
