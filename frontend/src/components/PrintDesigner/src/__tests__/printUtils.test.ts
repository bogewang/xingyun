import { describe, expect, it } from 'vitest';

import { resetPageNumberForEachPrintData } from '../printUtils';

describe('批量单据打印模板', () => {
  it('为每个面板关闭页码续排且不修改原模板', () => {
    const template = {
      panels: [
        { index: 0, paperNumberContinue: true },
        { index: 1, paperNumberContinue: true },
      ],
    };

    const result = resetPageNumberForEachPrintData(template);

    expect(result.panels).toEqual([
      { index: 0, paperNumberContinue: false },
      { index: 1, paperNumberContinue: false },
    ]);
    expect(template.panels[0].paperNumberContinue).toBe(true);
    expect(template.panels[1].paperNumberContinue).toBe(true);
  });
});
