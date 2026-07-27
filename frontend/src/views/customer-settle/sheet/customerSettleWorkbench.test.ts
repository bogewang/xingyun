// @vitest-environment jsdom

import { describe, expect, it, vi } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { reactive } from 'vue';
import { CUSTOMER_SALE_SETTLE_BIZ_TYPE } from '@/enums/biz/customerSaleSettleBizType';
import {
  buildCustomerDetailQuery,
  buildDirectSettlePayload,
  canDirectSettle,
  canSelectDirectSettleRow,
  getCustomerSettleBizListPath,
  isDirectSettleAmountValid,
  queryCustomerSettleWorkbenchPages,
  validateCustomerDetailRoute,
} from './customerSettleWorkbench';
import CustomerSettleDetail from './settle.vue';
import * as sheetApi from '@/api/customer-settle/sheet';

vi.mock('@/mixins/multiplePageMix', () => ({
  multiplePageMix: {},
}));

vi.mock('@/hooks/web/msg', () => ({
  createError: vi.fn(),
  createSuccess: vi.fn(),
}));

vi.mock('@/api/customer-settle/check', () => ({}));
vi.mock('@/api/customer-settle/sheet', () => ({
  querySaleSettleInfos: vi.fn(),
}));

/** 创建可由测试控制完成时机的异步请求。 */
function createDeferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((currentResolve, currentReject) => {
    resolve = currentResolve;
    reject = currentReject;
  });
  return { promise, resolve, reject };
}

/** 创建用于调用详情页加载方法的最小组件上下文。 */
function createLoadListContext(customerId: string) {
  const context = {
    $route: { query: { customerId } },
    $refs: { grid: { getCheckboxRecords: vi.fn(() => []) } },
    $nextTick: (callback: () => void) => callback(),
    customerId,
    routeError: undefined as string | undefined,
    loading: false,
    loadRequestSequence: 0,
    tableData: [] as any[],
    selectedRows: [] as any[],
    pagerConfig: { total: 0 },
    ensureValidRoute: vi.fn(() => true),
    buildQueryParams: vi.fn(() => ({ pageIndex: 1, pageSize: 20, bizType: 1 })),
    syncSelection: vi.fn(),
  };
  return context;
}

describe('客户结算工作台', () => {
  it('状态枚举在响应式页面数据中仍可解析状态文案', () => {
    const pageData = (CustomerSettleDetail as any).data.call({
      $route: { query: { customerId: 'C1' } },
    });
    const reactivePageData = reactive(pageData);

    expect(() => reactivePageData.SETTLE_STATUS.getDesc(0)).not.toThrow();
  });

  it('详情页根节点不使用会直接删除 DOM 的权限指令', () => {
    const detailSource = readFileSync(resolve(__dirname, 'settle.vue'), 'utf8');

    expect(detailSource).not.toContain(
      '<div v-permission="[\'customer-settle:sheet:query\']" class="customer-settle-detail">',
    );
  });

  it('固定路由客户并拒绝缺失客户参数的详情页请求', () => {
    expect(buildCustomerDetailQuery({ customerId: 'C1' }, { customerId: 'C2' })).toMatchObject({
      customerId: 'C1',
    });
    expect(validateCustomerDetailRoute({})).toBe('客户参数不能为空！');
  });

  it('只展示可结算的销售出库和销售退货类型', () => {
    expect(CUSTOMER_SALE_SETTLE_BIZ_TYPE.values()).toHaveLength(2);
    expect(CUSTOMER_SALE_SETTLE_BIZ_TYPE.getDesc(1)).toBe('销售出库单');
    expect(CUSTOMER_SALE_SETTLE_BIZ_TYPE.getDesc(2)).toBe('销售退单');
  });

  it('拒绝勾选不同客户的单据', () => {
    expect(canDirectSettle([{ customerId: 'C1' }, { customerId: 'C2' }])).toBe(false);
  });

  it('只允许待结算或部分结算的同一客户单据', () => {
    expect(
      canDirectSettle([
        { customerId: 'C1', settleStatus: 'UN_SETTLE' },
        { customerId: 'C1', settleStatus: 'PART_SETTLE' },
      ]),
    ).toBe(true);
  });

  it('禁止勾选其他客户或已结算单据', () => {
    const selectedRows = [{ id: 'S1', customerId: 'C1', settleStatus: 0 }];

    expect(
      canSelectDirectSettleRow({ id: 'S2', customerId: 'C2', settleStatus: 0 }, selectedRows),
    ).toBe(false);
    expect(
      canSelectDirectSettleRow({ id: 'S3', customerId: 'C1', settleStatus: 3 }, selectedRows),
    ).toBe(false);
  });

  it('构造直接结算 payload', () => {
    expect(
      buildDirectSettlePayload(
        [
          { id: 'S1', customerId: 'C1', bizType: 1, unSettleAmount: 60, checkAmount: 50 },
          { id: 'S2', customerId: 'C1', bizType: 2, unSettleAmount: -20, checkAmount: -10 },
        ],
        88.5,
        '',
      ),
    ).toEqual({
      customerId: 'C1',
      settleAmount: 88.5,
      description: undefined,
      items: [
        { bizId: 'S1', bizType: 1, unSettleAmount: 60, checkAmount: 50 },
        { bizId: 'S2', bizType: 2, unSettleAmount: -20, checkAmount: -10 },
      ],
    });
  });

  it('路由客户变更时清空旧数据并重新查询，缺失客户参数时阻断请求', () => {
    const search = vi.fn();
    const clearRouteData = vi.fn();
    const context = {
      $route: { name: 'CustomerSettleDetail' },
      routeError: undefined,
      searchFormData: { customerId: 'C1' },
      search,
      clearRouteData,
      handleRouteQueryChange: (CustomerSettleDetail as any).methods.handleRouteQueryChange,
    };
    const routeWatcher = (CustomerSettleDetail as any).watch['$route.query'];

    routeWatcher.handler.call(context, { customerId: 'C2' }, { customerId: 'C1' });
    expect(clearRouteData).toHaveBeenCalledTimes(1);
    expect(search).toHaveBeenCalledTimes(1);
    expect(context.searchFormData.customerId).toBe('C2');

    routeWatcher.handler.call(context, {}, { customerId: 'C2' });
    expect(context.routeError).toBe('客户参数不能为空！');
    expect(clearRouteData).toHaveBeenCalledTimes(2);
    expect(search).toHaveBeenCalledTimes(1);
  });

  it('切换到其他页签时不校验客户结算路由参数', () => {
    const handleRouteQueryChange = vi.fn();
    const routeWatcher = (CustomerSettleDetail as any).watch['$route.query'];
    const context = {
      $route: { name: 'SupplierSettleSummary' },
      handleRouteQueryChange,
    };

    routeWatcher.handler.call(context, {}, { customerId: 'C1' });

    expect(handleRouteQueryChange).not.toHaveBeenCalled();
  });

  it('未指定业务类型时并行查询销售出库和退货并保留混合选单类型', async () => {
    const query = vi.fn(async (params: { bizType?: number }) => ({
      pageIndex: 1,
      pageSize: 20,
      totalCount: 1,
      datas: [
        {
          id: params.bizType === 1 ? 'S1' : 'R1',
          customerId: 'C1',
          bizType: params.bizType,
          settleStatus: 0,
        },
      ],
    }));

    const result = await queryCustomerSettleWorkbenchPages({ pageIndex: 1, pageSize: 20 }, query);

    expect(query).toHaveBeenCalledTimes(2);
    expect(query).toHaveBeenNthCalledWith(1, { pageIndex: 1, pageSize: 20, bizType: 1 });
    expect(query).toHaveBeenNthCalledWith(2, { pageIndex: 1, pageSize: 20, bizType: 2 });
    expect(result.totalCount).toBe(2);
    expect(result.datas.map((item) => item.bizType)).toEqual([1, 2]);
    expect(
      buildDirectSettlePayload(
        result.datas as Required<
          Pick<(typeof result.datas)[number], 'id' | 'customerId' | 'bizType'>
        >[],
        80,
        '',
      ).items,
    ).toEqual([
      { bizId: 'S1', bizType: 1, unSettleAmount: 0, checkAmount: 0 },
      { bizId: 'R1', bizType: 2, unSettleAmount: 0, checkAmount: 0 },
    ]);
  });

  it('允许同方向的对账或结算金额增减，并拒绝零金额或方向不一致', () => {
    expect(isDirectSettleAmountValid(-20, -20)).toBe(true);
    expect(isDirectSettleAmountValid(-10, -20)).toBe(true);
    expect(isDirectSettleAmountValid(-30, -20)).toBe(true);
    expect(isDirectSettleAmountValid(30, 20)).toBe(true);
    expect(isDirectSettleAmountValid(0, -20)).toBe(false);
    expect(isDirectSettleAmountValid(10, -20)).toBe(false);
  });

  it('按业务类型解析销售单据跳转路径', () => {
    expect(getCustomerSettleBizListPath(1)).toBe('/sale/out');
    expect(getCustomerSettleBizListPath(2)).toBe('/sale/return');
    expect(getCustomerSettleBizListPath(3)).toBeUndefined();
  });

  it('旧客户请求失败时不清空新客户已加载的数据或关闭其加载状态', async () => {
    const previousRequest = createDeferred<any>();
    const currentRequest = createDeferred<any>();
    vi.mocked(sheetApi.querySaleSettleInfos)
      .mockReturnValueOnce(previousRequest.promise as any)
      .mockReturnValueOnce(currentRequest.promise as any);
    const context = createLoadListContext('C1');
    const loadList = (CustomerSettleDetail as any).methods.loadList;

    const previousLoad = loadList.call(context);
    context.customerId = 'C2';
    context.$route.query.customerId = 'C2';
    const currentLoad = loadList.call(context);

    context.tableData = [{ id: 'C2-EXISTING' }];
    context.selectedRows = [{ id: 'C2-SELECTED' }];
    previousRequest.reject(new Error('旧请求失败'));
    await previousLoad;

    expect(context.tableData).toEqual([{ id: 'C2-EXISTING' }]);
    expect(context.selectedRows).toEqual([{ id: 'C2-SELECTED' }]);
    expect(context.loading).toBe(true);

    currentRequest.resolve({ datas: [{ id: 'C2-NEW' }], totalCount: 1 });
    await currentLoad;
    expect(context.tableData).toEqual([{ id: 'C2-NEW' }]);
    expect(context.pagerConfig.total).toBe(1);
    expect(context.loading).toBe(false);
  });

  it('C1 到 C2 再到 C1 时仅最后一次 C1 请求可以写入数据', async () => {
    const firstC1Request = createDeferred<any>();
    const c2Request = createDeferred<any>();
    const lastC1Request = createDeferred<any>();
    vi.mocked(sheetApi.querySaleSettleInfos)
      .mockReturnValueOnce(firstC1Request.promise as any)
      .mockReturnValueOnce(c2Request.promise as any)
      .mockReturnValueOnce(lastC1Request.promise as any);
    const context = createLoadListContext('C1');
    const loadList = (CustomerSettleDetail as any).methods.loadList;

    const firstC1Load = loadList.call(context);
    context.customerId = 'C2';
    context.$route.query.customerId = 'C2';
    const c2Load = loadList.call(context);
    context.customerId = 'C1';
    context.$route.query.customerId = 'C1';
    const lastC1Load = loadList.call(context);

    lastC1Request.resolve({ datas: [{ id: 'C1-LATEST' }], totalCount: 1 });
    await lastC1Load;
    firstC1Request.resolve({ datas: [{ id: 'C1-STALE' }], totalCount: 1 });
    c2Request.resolve({ datas: [{ id: 'C2-STALE' }], totalCount: 1 });
    await Promise.all([firstC1Load, c2Load]);

    expect(context.tableData).toEqual([{ id: 'C1-LATEST' }]);
    expect(context.pagerConfig.total).toBe(1);
  });

  it('新路由请求开始后，旧请求的 nextTick 不同步勾选状态', async () => {
    const previousRequest = createDeferred<any>();
    const currentRequest = createDeferred<any>();
    vi.mocked(sheetApi.querySaleSettleInfos)
      .mockReturnValueOnce(previousRequest.promise as any)
      .mockReturnValueOnce(currentRequest.promise as any);
    const context = createLoadListContext('C1');
    const nextTickCallbacks: Array<() => void> = [];
    context.$nextTick = (callback: () => void) => nextTickCallbacks.push(callback);
    const loadList = (CustomerSettleDetail as any).methods.loadList;

    const previousLoad = loadList.call(context);
    previousRequest.resolve({ datas: [{ id: 'C1-OLD' }], totalCount: 1 });
    await previousLoad;

    context.customerId = 'C2';
    context.$route.query.customerId = 'C2';
    const currentLoad = loadList.call(context);

    nextTickCallbacks.shift()?.();

    expect(context.syncSelection).not.toHaveBeenCalled();

    currentRequest.resolve({ datas: [{ id: 'C2-NEW' }], totalCount: 1 });
    await currentLoad;
  });
});
