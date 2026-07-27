// @vitest-environment jsdom

import { describe, expect, it } from 'vitest';
import { CustomerSettleRoute } from './index';

describe('客户结算路由', () => {
  it('注册客户结算总览与明细页面', () => {
    const routes = CustomerSettleRoute.children || [];

    expect(routes.find((route) => route.path === 'customer/sheet')?.name).toBe(
      'CustomerSettleOverview',
    );
    expect(routes.find((route) => route.path === 'customer/sheet-detail')?.name).toBe(
      'CustomerSettleDetail',
    );
  });

  it('短路径打开总览并兼容旧工作台路径', () => {
    const overview = CustomerSettleRoute.children?.find(
      (route) => route.name === 'CustomerSettleOverview',
    );
    const legacyWorkbench = CustomerSettleRoute.children?.find(
      (route) => route.path === 'customer/sheet/settle',
    );

    expect(overview?.path).toBe('customer/sheet');
    expect(legacyWorkbench?.redirect).toBe('/settle/customer/sheet');
  });
});
