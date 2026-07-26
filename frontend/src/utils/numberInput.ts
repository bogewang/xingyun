/**
 * 清理数量、金额输入，仅保留非负小数。
 */
export function sanitizeNonNegativeDecimalInput(
  value: string | number | null | undefined,
  previousValue = '',
): string {
  const input = String(value ?? '');
  if (input === '') {
    return '';
  }

  if (!/^(?:\d+|\d*\.\d*)$/.test(input)) {
    return previousValue;
  }

  return input.startsWith('.') ? `0${input}` : input;
}

/**
 * 清理数量、金额输入，允许负数（保留可选前导负号）。
 */
export function sanitizeDecimalInput(
  value: string | number | null | undefined,
  previousValue = '',
): string {
  const input = String(value ?? '');
  if (input === '') {
    return '';
  }

  if (!/^-?(?:\d+|\d*\.\d*)$/.test(input)) {
    return previousValue;
  }

  if (input === '-') {
    return '-';
  }

  return input.startsWith('-.') ? `-0${input.slice(1)}` : input;
}
