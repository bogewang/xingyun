import { describe, expect, it, vi } from 'vitest';
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
import CustomerSettleDetail from './detail.vue';

vi.mock('@/mixins/multiplePageMix', () => ({
  multiplePageMix: {},
}));

vi.mock('@/hooks/web/msg', () => ({
  createError: vi.fn(),
  createSuccess: vi.fn(),
}));

vi.mock('@/api/customer-settle/check', () => ({}));
vi.mock('@/api/customer-settle/sheet', () => ({}));

describe('客户结算工作台', () => {
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
      routeError: undefined,
      search,
      clearRouteData,
      handleRouteQueryChange: (CustomerSettleDetail as any).methods.handleRouteQueryChange,
    };
    const routeWatcher = (CustomerSettleDetail as any).watch['$route.query'];

    routeWatcher.handler.call(context, { customerId: 'C2' }, { customerId: 'C1' });
    expect(clearRouteData).toHaveBeenCalledTimes(1);
    expect(search).toHaveBeenCalledTimes(1);

    routeWatcher.handler.call(context, {}, { customerId: 'C2' });
    expect(context.routeError).toBe('客户参数不能为空！');
    expect(clearRouteData).toHaveBeenCalledTimes(2);
    expect(search).toHaveBeenCalledTimes(1);
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

  it('允许退货负数退款并拒绝零金额或方向不一致', () => {
    expect(isDirectSettleAmountValid(-20, -20)).toBe(true);
    expect(isDirectSettleAmountValid(-10, -20)).toBe(true);
    expect(isDirectSettleAmountValid(0, -20)).toBe(false);
    expect(isDirectSettleAmountValid(10, -20)).toBe(false);
  });

  it('按业务类型解析销售单据跳转路径', () => {
    expect(getCustomerSettleBizListPath(1)).toBe('/sale/out');
    expect(getCustomerSettleBizListPath(2)).toBe('/sale/return');
    expect(getCustomerSettleBizListPath(3)).toBeUndefined();
  });
});
