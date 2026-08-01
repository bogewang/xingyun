import { add, getNumber, isEmpty } from '@/utils/utils';
import { calculateSheetLineAmount } from '@/utils/sheetAmountInput';

export interface SaleOutConfirmRow {
  confirmNum?: number | string | null;
  confirmAmt?: number | string | null;
  taxPrice?: number | string | null;
}

/**
 * 将验收数量归一为数字，供提交参数使用。
 * @param confirmNum 验收数量
 * @returns 数字类型验收数量
 */
export function normalizeConfirmNum(confirmNum?: number | string | null): number {
  return isEmpty(confirmNum) ? 0 : getNumber(confirmNum, 6);
}

/**
 * 计算单行验收金额，空值按 0 处理。
 * @param row 验收行数据
 * @returns 单行验收金额
 */
export function getConfirmAmount(row: SaleOutConfirmRow): number {
  const confirmNum = normalizeConfirmNum(row?.confirmNum);
  const taxPrice = isEmpty(row?.taxPrice) ? 0 : row.taxPrice;
  return calculateSheetLineAmount(confirmNum, taxPrice);
}

/**
 * 计算单行验收金额并同步写回行数据，供编辑器展示使用。
 * @param row 验收行数据
 * @returns 同步后的单行验收金额
 */
export function syncConfirmAmount(row: SaleOutConfirmRow): number {
  const confirmAmt = getConfirmAmount(row);
  row.confirmAmt = confirmAmt;
  return confirmAmt;
}

/**
 * 将验收金额格式化为两位小数，供金额字段展示使用。
 * @param confirmAmt 验收金额
 * @returns 两位小数的验收金额文本
 */
export function formatConfirmAmount(confirmAmt?: number | string | null): string {
  const amount = Number(confirmAmt ?? 0);
  return Number.isFinite(amount) ? amount.toFixed(2) : '0.00';
}

/**
 * 汇总验收数量和验收金额，空值按 0 处理。
 * @param rows 验收明细列表
 * @returns 汇总结果
 */
export function sumConfirmFields(rows: SaleOutConfirmRow[]): {
  confirmNum: number;
  confirmAmt: number;
} {
  return rows.reduce(
    (totals, row) => ({
      confirmNum: add(totals.confirmNum, normalizeConfirmNum(row?.confirmNum)),
      confirmAmt: add(totals.confirmAmt, isEmpty(row?.confirmAmt) ? 0 : row.confirmAmt),
    }),
    { confirmNum: 0, confirmAmt: 0 },
  );
}
