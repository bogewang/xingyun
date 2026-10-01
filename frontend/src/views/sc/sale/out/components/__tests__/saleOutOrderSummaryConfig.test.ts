import { readFileSync } from 'node:fs';
import { describe, expect, it, vi } from 'vitest';

describe('订单汇总按钮配置', () => {
  // 执行真实加载方法，验证参数值及请求失败时按钮状态。
  it('读取开关并更新按钮显示状态', async () => {
    const source = readFileSync(new URL('../sheet-list.vue', import.meta.url), 'utf-8');
    expect(source).toMatch(/v-if="showOrderSummaryExport"[\s\S]*?@click="exportOrderSummary"/);
    const start = source.indexOf('async loadOrderSummaryExportConfig()');
    const end = source.indexOf('      /** 加载合并商品功能开关', start);
    const api = { getOrderSummaryExportConfig: vi.fn() };
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    try {
      const load = new Function(
        'api',
        `return ({ ${source.slice(start, end)} }).loadOrderSummaryExportConfig;`,
      )(api);
      const context = { showOrderSummaryExport: false };
      api.getOrderSummaryExportConfig.mockResolvedValueOnce(true);
      await load.call(context);
      expect(context.showOrderSummaryExport).toBe(true);
      api.getOrderSummaryExportConfig.mockResolvedValueOnce(false);
      await load.call(context);
      expect(context.showOrderSummaryExport).toBe(false);
      context.showOrderSummaryExport = true;
      api.getOrderSummaryExportConfig.mockRejectedValueOnce(new Error('配置读取失败'));
      await load.call(context);
      expect(context.showOrderSummaryExport).toBe(false);
      expect(warn).toHaveBeenCalledOnce();
    } finally {
      warn.mockRestore();
    }
  });
});
