import { UpdateSupplierAvailableVo } from '@/api/base-data/supplier/model/updateSupplierAvailableVo';

/**
 * 构建供应商批量状态请求参数。
 *
 * @param records 供应商列表记录
 * @param available 目标状态
 */
export function buildSupplierAvailabilityRequest(
  records: Array<{ id: string }>,
  available: boolean,
): UpdateSupplierAvailableVo {
  return {
    ids: [...new Set(records.map((item) => item.id).filter(Boolean))],
    available,
  };
}
