// @vitest-environment jsdom

import { describe, expect, it } from 'vitest';
import { CustomerSettleRoute } from './index';

describe('客户结算路由', () => {
  it('短路径打开工作台并兼容旧工作台路径', () => {
    const workbench = CustomerSettleRoute.children?.find(
      (route) => route.name === 'CustomerSettleWorkbench',
    );
    const legacyWorkbench = CustomerSettleRoute.children?.find(
      (route) => route.path === 'customer/sheet/settle',
    );

    expect(workbench?.path).toBe('customer/sheet');
    expect(legacyWorkbench?.redirect).toBe('/settle/customer/sheet');
  });
});
