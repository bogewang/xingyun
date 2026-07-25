import { describe, expect, it } from 'vitest';
import {
  buildDirectSettlePayload,
  canDirectSettle,
  canSelectDirectSettleRow,
  getCustomerSettleBizListPath,
} from './customerSettleWorkbench';

describe('客户结算工作台', () => {
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
      canSelectDirectSettleRow(
        { id: 'S2', customerId: 'C2', settleStatus: 0 },
        selectedRows,
      ),
    ).toBe(false);
    expect(
      canSelectDirectSettleRow(
        { id: 'S3', customerId: 'C1', settleStatus: 3 },
        selectedRows,
      ),
    ).toBe(false);
  });

  it('构造直接结算 payload', () => {
    expect(
      buildDirectSettlePayload(
        [
          { id: 'S1', customerId: 'C1', bizType: 1 },
          { id: 'S2', customerId: 'C1', bizType: 2 },
        ],
        88.5,
        '',
      ),
    ).toEqual({
      customerId: 'C1',
      settleAmount: 88.5,
      description: undefined,
      items: [
        { bizId: 'S1', bizType: 1 },
        { bizId: 'S2', bizType: 2 },
      ],
    });
  });

  it('按业务类型解析销售单据跳转路径', () => {
    expect(getCustomerSettleBizListPath(1)).toBe('/sale/out');
    expect(getCustomerSettleBizListPath(2)).toBe('/sale/return');
    expect(getCustomerSettleBizListPath(3)).toBeUndefined();
  });
});
