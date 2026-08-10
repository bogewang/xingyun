import { describe, expect, it } from 'vitest';
import { buildVisibleSelectOptions, normalizeSelectValue } from '../searchSelect';

describe('搜索下拉选项', () => {
  const optionMap = {
    customer1: { label: '客户一', value: 'customer1' },
    customer2: { label: '客户二', value: 'customer2' },
  };

  it('多选时仅保留已加载的有效选项', () => {
    expect(normalizeSelectValue(['customer1', 'missing'], optionMap)).toEqual(['customer1']);
  });

  it('异步搜索时保留全部已选项', () => {
    expect(
      buildVisibleSelectOptions(['customer1', 'customer2'], optionMap, [
        { label: '客户三', value: 'customer3' },
      ]),
    ).toEqual([
      { label: '客户一', value: 'customer1' },
      { label: '客户二', value: 'customer2' },
      { label: '客户三', value: 'customer3' },
    ]);
  });

  it('兼容原有单选值处理', () => {
    expect(normalizeSelectValue('customer1', optionMap)).toBe('customer1');
    expect(buildVisibleSelectOptions('customer1', optionMap, [])).toEqual([
      { label: '客户一', value: 'customer1' },
    ]);
  });
});
