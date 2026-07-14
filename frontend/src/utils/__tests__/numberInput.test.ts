import { describe, expect, it } from 'vitest';

import { sanitizeNonNegativeDecimalInput } from '../numberInput';

describe('非负金额输入清理', () => {
  it('拒绝非法字符并保留上一次合法值', () => {
    expect(sanitizeNonNegativeDecimalInput('a-12.3.4b', '8.5')).toBe('8.5');
    expect(sanitizeNonNegativeDecimalInput('-12', '8.5')).toBe('8.5');
    expect(sanitizeNonNegativeDecimalInput('1e3', '8.5')).toBe('8.5');
  });

  it('保留合法小数输入并规范化小数点开头', () => {
    expect(sanitizeNonNegativeDecimalInput('12.50')).toBe('12.50');
    expect(sanitizeNonNegativeDecimalInput('.5')).toBe('0.5');
  });

  it('允许清空输入框', () => {
    expect(sanitizeNonNegativeDecimalInput('')).toBe('');
  });
});
