import { describe, expect, it } from 'vitest';

import { isPrintDotTemplate, toPrintDotVariables } from './printdot';

describe('PrintDot 模板适配', () => {
  it('接受包含 data 字段的 PrintDot 模板', () => {
    expect(isPrintDotTemplate({ data: {} })).toBe(true);
  });

  it('拒绝旧版 panels 模板', () => {
    expect(isPrintDotTemplate({ panels: [] })).toBe(false);
  });

  it('将数组业务数据的第一项转换为变量对象', () => {
    expect(toPrintDotVariables([{ orderNo: 'SO-001' }, { orderNo: 'SO-002' }])).toEqual({
      orderNo: 'SO-001',
    });
  });
});
