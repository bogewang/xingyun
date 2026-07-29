import { getNumber, isFloat, isFloatGeZero, mul } from '@/utils/utils';

export interface SaleOutProfitRow {
  taxPrice?: number | string | null;
  taxAmount?: number | string | null;
  outNum?: number | string | null;
  confirmNum?: number | string | null;
  confirmAmt?: number | string | null;
  costPrice?: number | string | null;
  totalProfit?: number | string | null;
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

/** 获取毛利计算基数，验收金额非零时优先使用验收金额。 */
export function getSaleOutProfitBaseAmount(row: SaleOutProfitRow): number {
  const confirmAmt = Number(row?.confirmAmt);
  return Number.isFinite(confirmAmt) && confirmAmt !== 0 ? confirmAmt : getSaleOutAmount(row);
}

/** 获取成本计算数量，验收数量非零时优先使用验收数量。 */
export function getSaleOutCostQuantity(row: SaleOutProfitRow): number {
  const confirmNum = Number(row?.confirmNum);
  return Number.isFinite(confirmNum) && confirmNum !== 0 ? confirmNum : Number(row?.outNum || 0);
}

/** 根据后端返回的利润计算毛利率。 */
export function calcSaleOutProfitRateByProfit(
  profit: number | string | null | undefined,
  saleAmount: number | string | null | undefined,
  confirmAmt: number | string | null | undefined,
): string {
  const confirmAmount = Number(confirmAmt || 0);
  const baseAmount = confirmAmount !== 0 ? confirmAmount : Number(saleAmount || 0);
  if (!baseAmount) {
    return '0.00%';
  }

  return `${((Number(profit || 0) / baseAmount) * 100).toFixed(2)}%`;
}

/** 根据前端金额和成本计算毛利率，验收金额为零时使用销售金额作为基数。 */
export function calcSaleOutProfitRateByCost(
  saleAmount: number | string | null | undefined,
  confirmAmt: number | string | null | undefined,
  costAmount: number | string | null | undefined,
): string {
  const confirmAmount = Number(confirmAmt || 0);
  const baseAmount = confirmAmount !== 0 ? confirmAmount : Number(saleAmount || 0);
  if (!baseAmount) {
    return '0.00%';
  }

  return `${(((baseAmount - Number(costAmount || 0)) / baseAmount) * 100).toFixed(2)}%`;
}

/**
 * 计算销售出库明细利润额，利润按验收金额减出库成本。
 * @param row 销售出库明细行
 * @returns 利润额
 */
export function calcSaleOutProfitAmount(row: SaleOutProfitRow): number {
  const amt = getSaleOutProfitBaseAmount(row);
  const costAmount = Number(getNumber(mul(row?.costPrice || 0, getSaleOutCostQuantity(row)), 2));
  return amt - costAmount;
}

/**
 * 计算销售出库明细毛利率，按售价与成本单价计算。
 * @param row 销售出库明细行
 * @returns 毛利率文本
 */
export function calcSaleOutProfitRate(row: SaleOutProfitRow): string {
  const salePrice = Number(row?.taxPrice || 0);
  const costPrice = Number(row?.costPrice || 0);
  if (!salePrice || !Number.isFinite(salePrice) || !Number.isFinite(costPrice)) {
    return '0.00%';
  }

  return `${(((salePrice - costPrice) / salePrice) * 100).toFixed(2)}%`;
}

/**
 * 判断销售出库明细是否为负毛利。
 * @param row 销售出库明细行
 * @returns 是否负毛利
 */
export function isSaleOutProfitNegative(row: SaleOutProfitRow): boolean {
  if (!isFloatGeZero(row?.taxPrice) || !isFloatGeZero(row?.costPrice) || !isFloat(row?.outNum)) {
    return false;
  }

  if (row?.totalProfit !== null && row?.totalProfit !== undefined && row?.totalProfit !== '') {
    return Number(row.totalProfit) < 0;
  }

  return calcSaleOutProfitAmount(row) < 0;
}
