import { div, mul } from '@/utils/utils';

/**
 * 计算当前单位对应的单价。
 *
 * @param unitPrice 当前单位单价
 * @param baseUnitPrice 已缓存的主单位单价
 * @param oldConversionRate 原单位换算率
 * @param newConversionRate 新单位换算率
 * @return 新单位单价
 */
export function calculateUnitPrice(
  unitPrice: number | string | null | undefined,
  baseUnitPrice: number | string | null | undefined,
  oldConversionRate: number | string | null | undefined,
  newConversionRate: number | string | null | undefined,
) {
  const oldRate = Number(oldConversionRate) || 1;
  const newRate = Number(newConversionRate) || 1;
  const basePrice =
    baseUnitPrice !== null && baseUnitPrice !== undefined
      ? baseUnitPrice
      : div(Number(unitPrice) || 0, oldRate || 1);

  return {
    basePrice,
    unitPrice: mul(basePrice || 0, newRate),
  };
}
