import { UpdateProductAvailableVo } from '@/api/base-data/product/info/model/updateProductAvailableVo';

/**
 * 构建商品批量状态请求参数。
 *
 * @param records 商品列表记录
 * @param available 目标状态
 */
export function buildProductAvailabilityRequest(
  records: Array<{ id: string }>,
  available: boolean,
): UpdateProductAvailableVo {
  return {
    ids: [...new Set(records.map((item) => item.id).filter(Boolean))],
    available,
  };
}
