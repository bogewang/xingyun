// @vitest-environment jsdom

import { describe, expect, it } from 'vitest';
import { CustomerSettleRoute, QuoteRoute, SettleRoute } from './index';

describe('客户结算路由', () => {
  it('注册客户结算汇总与具体结算页面', () => {
    const routes = CustomerSettleRoute.children || [];

    expect(routes.find((route) => route.path === 'customer/sheet')?.name).toBe(
      'CustomerSettleOverview',
    );
    expect(routes.find((route) => route.path === 'customer/settle')?.name).toBe(
      'CustomerSettleDetail',
    );
  });

  it('使用 /sheet 作为客户结算汇总路径', () => {
    const overview = CustomerSettleRoute.children?.find(
      (route) => route.name === 'CustomerSettleOverview',
    );

    expect(overview?.path).toBe('customer/sheet');
    expect(overview?.meta?.title).toBe('客户结算汇总');
  });

  it('使用 /sheet 作为供应商结算汇总路径、/settle 作为具体结算路径', () => {
    const routes = SettleRoute.children || [];

    expect(routes.find((route) => route.name === 'SupplierSettleSummary')?.path).toBe(
      'supplier/sheet',
    );
    expect(routes.find((route) => route.name === 'AddSupplierSettleSheet')?.path).toBe(
      'supplier/settle',
    );
  });
});

describe('报价单路由', () => {
  it('注册报价单管理、详情、新增与修改页面', () => {
    const routes = QuoteRoute.children || [];

    expect(routes.find((route) => route.path === 'quote')?.name).toBe('QuoteSheet');
    expect(routes.find((route) => route.path === 'quote/detail/:id')?.name).toBe(
      'QuoteSheetDetail',
    );
    expect(routes.find((route) => route.path === 'quote/add')?.name).toBe('QuoteSheetAdd');
    expect(routes.find((route) => route.path === 'quote/modify/:id')?.name).toBe(
      'QuoteSheetModify',
    );
  });
});
