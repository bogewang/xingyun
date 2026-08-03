import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

describe('sale out sheet list amount display', () => {
  it('formats the acceptance amount column with two decimal places', () => {
    const source = readFileSync(new URL('../sheet-list.vue', import.meta.url), 'utf-8');
    const confirmAmountColumn = source.match(
      /field:\s*'confirmAmt'[\s\S]{0,300}?(?=\n\s*\{\s*field:|\n\s*\{\s*type:|\n\s*\],)/,
    )?.[0];

    expect(confirmAmountColumn).toContain(
      'formatter: ({ cellValue }) => this.formatAmount(cellValue)',
    );
  });
});
