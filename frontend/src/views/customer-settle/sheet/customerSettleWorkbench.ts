/** 直接结算工作台行数据。 */
export interface DirectSettleRow {
  id?: string;
  customerId?: string;
  bizType?: number;
  settleStatus?: number | string;
}

/** 判断勾选的单据是否允许直接结算。 */
export function canDirectSettle(rows: DirectSettleRow[]): boolean {
  if (!rows.length) {
    return false;
  }

  const customerIds = new Set(rows.map((item) => item.customerId).filter(Boolean));
  const allowedStatuses = new Set([0, 1, '0', '1', 'UN_SETTLE', 'PART_SETTLE']);
  return customerIds.size === 1 && rows.every((item) => allowedStatuses.has(item.settleStatus));
}

/** 判断候选单据是否可被勾选。 */
export function canSelectDirectSettleRow(
  row: DirectSettleRow,
  selectedRows: DirectSettleRow[],
): boolean {
  return canDirectSettle([
    ...selectedRows.filter((item) => item.id !== row.id),
    row,
  ]);
}

/** 构造直接结算接口请求。 */
export function buildDirectSettlePayload(
  selectedRows: Required<Pick<DirectSettleRow, 'id' | 'customerId' | 'bizType'>>[],
  settleAmount: number,
  description: string,
) {
  return {
    customerId: selectedRows[0].customerId,
    settleAmount,
    description: description || undefined,
    items: selectedRows.map(({ id, bizType }) => ({ bizId: id, bizType })),
  };
}

/** 按业务类型返回销售单据列表路由。 */
export function getCustomerSettleBizListPath(bizType?: number): string | undefined {
  if (bizType === 1) {
    return '/sale/out';
  }
  if (bizType === 2) {
    return '/sale/return';
  }
  return undefined;
}
