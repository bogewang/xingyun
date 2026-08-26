import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

/**
 * 读取行内商品选择组件源代码。
 */
function readInlineProductSelectSource() {
  return readFileSync(resolve(__dirname, '..', 'inline-product-select.vue'), 'utf-8');
}

describe('行内商品选择热度展示', () => {
  it('热度星标组件已注册，能够渲染为图标', () => {
    const source = readInlineProductSelectSource();

    expect(source).toMatch(/components:\s*\{\s*StarTwoTone,\s*\}/);
    expect(source).toContain('<StarTwoTone');
  });
});
