import { describe, expect, it } from 'vitest';
import { canDirectSettle } from './settle';

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
});
