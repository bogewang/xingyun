import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const componentScss = readFileSync(
  fileURLToPath(new URL('../component.scss', import.meta.url)),
  'utf8',
);

describe('VXE table editable component styles', () => {
  it('makes the inline product selector flush with its table cell', () => {
    expect(componentScss).toContain(
      '.vxe-body--column:has(> :is(.vxe-cell, .vxe-tree-cell) > .inline-product-select > .ant-select)',
    );
    expect(componentScss).toContain(
      '.vxe-cell:has(> .inline-product-select > .ant-select),\n' +
        '  .vxe-tree-cell:has(> .inline-product-select > .ant-select)',
    );
    expect(componentScss).toContain('& > .inline-product-select');
  });
});
