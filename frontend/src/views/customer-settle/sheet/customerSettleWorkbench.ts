/** 直接结算工作台行数据。 */
export interface DirectSettleRow {
  id?: string;
  customerId?: string;
  bizType?: number;
  settleStatus?: number | string;
  unSettleAmount?: number;
  checkAmount?: number;
}

/** 校验客户结算明细页的固定客户路由参数。 */
export function validateCustomerDetailRoute(
  routeQuery: Record<string, unknown>,
): string | undefined {
  const customerId = String(routeQuery.customerId || '').trim();
  return customerId ? undefined : '客户参数不能为空！';
}

/** 使用路由客户 ID 覆盖页面中的客户查询条件。 */
export function buildCustomerDetailQuery<T extends Record<string, unknown>>(
  routeQuery: Record<string, unknown>,
  pageQuery: T,
): T & { customerId: string } {
  return {
    ...pageQuery,
    customerId: String(routeQuery.customerId || '').trim(),
  };
}

/** 客户结算工作台分页结果。 */
export interface CustomerSettleWorkbenchPage<T extends DirectSettleRow = DirectSettleRow> {
  pageIndex: number;
  pageSize: number;
  totalCount: number;
  datas: T[] | null;
  [key: string]: unknown;
}

/** 客户结算工作台查询函数。 */
export type CustomerSettleWorkbenchQuery<T extends DirectSettleRow = DirectSettleRow> = (
  params: Record<string, unknown> & { bizType: number },
) => Promise<CustomerSettleWorkbenchPage<T>>;

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
  return canDirectSettle([...selectedRows.filter((item) => item.id !== row.id), row]);
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
    items: selectedRows.map((row) => ({
      bizId: row.id,
      bizType: row.bizType,
      unSettleAmount: Number(row.unSettleAmount || 0),
      checkAmount: Number(row.checkAmount || 0),
    })),
  };
}

/** 判断确认金额与所选业务净额的方向及范围是否一致。 */
export function isDirectSettleAmountValid(amount: number, totalUnSettleAmount: number): boolean {
  if (!Number.isFinite(amount) || !Number.isFinite(totalUnSettleAmount)) {
    return false;
  }
  if (amount === 0 || totalUnSettleAmount === 0) {
    return false;
  }
  return (
    Math.sign(amount) === Math.sign(totalUnSettleAmount) &&
    Math.abs(amount) <= Math.abs(totalUnSettleAmount)
  );
}

/** 查询单一业务类型，或并行查询并合并销售出库与销售退货。 */
export async function queryCustomerSettleWorkbenchPages<T extends DirectSettleRow>(
  params: Record<string, unknown> & { bizType?: number },
  query: CustomerSettleWorkbenchQuery<T>,
): Promise<CustomerSettleWorkbenchPage<T>> {
  if (params.bizType === 1 || params.bizType === 2) {
    return query({ ...params, bizType: params.bizType });
  }
  const baseParams = { ...params };
  delete baseParams.bizType;
  const [saleOutPage, saleReturnPage] = await Promise.all([
    query({ ...baseParams, bizType: 1 }),
    query({ ...baseParams, bizType: 2 }),
  ]);
  return {
    ...saleOutPage,
    totalCount: Number(saleOutPage.totalCount || 0) + Number(saleReturnPage.totalCount || 0),
    datas: [...(saleOutPage.datas || []), ...(saleReturnPage.datas || [])],
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
