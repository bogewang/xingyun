import { describe, expect, it } from 'vitest';

import {
  buildPrintPayload,
  resetPageNumberForEachPrintData,
  shouldResetPageNumberForPrintData,
} from '../printUtils';

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

describe('打印份数数据构建', () => {
  it('按指定份数复制全部单据数据', () => {
    const data = [{ code: 'SO001' }, { code: 'SO002' }];

    expect(buildPrintPayload(data, 2)).toEqual([
      { code: 'SO001' },
      { code: 'SO002' },
      { code: 'SO001' },
      { code: 'SO002' },
    ]);
  });

  it('多份打印时重置每份单据的页码', () => {
    expect(shouldResetPageNumberForPrintData(false, 1)).toBe(false);
    expect(shouldResetPageNumberForPrintData(false, 2)).toBe(true);
    expect(shouldResetPageNumberForPrintData(true, 1)).toBe(true);
  });
});
