/** vxe-table 合计行所需的列配置。 */
export interface ReceiveSheetFooterColumn {
  type?: string;
  field?: string;
}

/** 采购收货单列表的合计行数据。 */
export interface ReceiveSheetFooterRow {
  [field: string]: unknown;
}

/**
 * 构建采购收货单列表的合计行。
 *
 * @param columns vxe-table 列配置
 * @param data 当前页列表数据
 * @returns 与列顺序一致的合计单元格文本
 */
export function buildReceiveSheetFooter(
  columns: ReceiveSheetFooterColumn[],
  data: ReceiveSheetFooterRow[],
): string[][] {
  const totalNum = sumByField(data, 'totalNum');
  const totalAmount = sumByField(data, 'totalAmount');
  const paidAmount = sumByField(data, 'paidAmount');
  const unpaidAmount = sumByField(data, 'unpaidAmount');

  return [
    columns.map((column) => {
      if (column.type === 'seq') {
        return '合计';
      }

      if (column.field === 'totalNum') {
        return formatQuantity(totalNum);
      }

      if (column.field === 'totalAmount') {
        return formatAmount(totalAmount);
      }

      if (column.field === 'paidAmount') {
        return formatAmount(paidAmount);
      }

      if (column.field === 'unpaidAmount') {
        return formatAmount(unpaidAmount);
      }

      return '';
    }),
  ];
}

/** 汇总指定字段的数值；单次解析失败或累计溢出为非有限数时，该字段当前累计回退为零。 */
function sumByField(data: ReceiveSheetFooterRow[], field: string): number {
  return (data || []).reduce((total, item) => {
    const sum = total + parseFiniteNumber(item?.[field]);
    return Number.isFinite(sum) ? sum : 0;
  }, 0);
}

/**
 * 解析用于合计的有限数值。
 *
 * 仅接受有限 number，或为兼容后端数字字符串而接受可解析为有限数的 string；
 * null、undefined、布尔值、数组、Symbol、Infinity 及非法字符串均按零处理，且不触发隐式转换。
 */
function parseFiniteNumber(value: unknown): number {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : 0;
  }

  if (typeof value === 'string') {
    const numberValue = Number(value);
    return Number.isFinite(numberValue) ? numberValue : 0;
  }

  return 0;
}

/** 将金额格式化为两位小数。 */
function formatAmount(value: number): string {
  return value.toFixed(2);
}

/** 将商品数量格式化为最多两位小数。 */
function formatQuantity(value: number): string {
  return value.toFixed(2).replace(/\.?0+$/, '');
}
