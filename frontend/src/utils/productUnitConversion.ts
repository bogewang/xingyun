import { div, getNumber, mul } from '@/utils/utils';

/**
 * 判断数值是否可以参与单位换算。
 *
 * @param value 待校验的数值
 * @return 是否为有效数字
 */
function isValidConversionNumber(value: number | string | null | undefined) {
  if (value === null || value === undefined || value === '') {
    return false;
  }

  return Number.isFinite(Number(value));
}

/**
 * 获取明细行当前单位的换算率。
 *
 * @param item 明细行数据
 * @return 当前单位换算率
 */
export function getUnitConversionRate(item) {
  return (
    Number(item?.conversionRate) ||
    Number((item?.units || []).find((unit) => unit.id === item?.unitId)?.conversionRate) ||
    1
  );
}

/**
 * 计算当前单位对应的单价。
 *
 * @param unitPrice 当前单位单价
 * @param baseUnitPrice 已缓存的主单位单价
 * @param oldConversionRate 原单位换算率
 * @param newConversionRate 新单位换算率
 * @return 新单位单价和主单位单价
 */
export function calculateUnitPrice(
  unitPrice: number | string | null | undefined,
  baseUnitPrice: number | string | null | undefined,
  oldConversionRate: number | string | null | undefined,
  newConversionRate: number | string | null | undefined,
) {
  const oldRate = Number(oldConversionRate) || 1;
  const newRate = Number(newConversionRate) || 1;
  const basePrice = isValidConversionNumber(baseUnitPrice)
    ? Number(baseUnitPrice)
    : div(Number(unitPrice) || 0, oldRate || 1);

  return {
    basePrice,
    unitPrice: mul(basePrice || 0, newRate),
  };
}

/**
 * 计算切换单位后的库存数量。
 *
 * @param stockNum 当前显示库存数量
 * @param baseStockNum 已缓存的主单位库存数量
 * @param oldConversionRate 原单位换算率
 * @param newConversionRate 新单位换算率
 * @return 新单位显示库存和主单位库存
 */
export function calculateUnitStockNum(
  stockNum: number | string | null | undefined,
  baseStockNum: number | string | null | undefined,
  oldConversionRate: number | string | null | undefined,
  newConversionRate: number | string | null | undefined,
) {
  const oldRate = Number(oldConversionRate) || 1;
  const newRate = Number(newConversionRate) || 1;
  const baseNum = isValidConversionNumber(baseStockNum)
    ? Number(baseStockNum)
    : mul(Number(stockNum) || 0, oldRate);

  return {
    baseStockNum: baseNum,
    stockNum: getNumber(div(baseNum || 0, newRate || 1), 6),
  };
}
