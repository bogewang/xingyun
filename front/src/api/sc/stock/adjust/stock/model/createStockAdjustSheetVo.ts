import { StockAdjustProductVo } from '@/api/sc/stock/adjust/stock/model/stockAdjustProductVo';

export interface CreateStockAdjustSheetVo {
  /**
   * 业务类型
   */
  bizType: number;

  /**
   * 调整原因ID
   */
  reasonId: string;

  /**
   * 备注
   */
  description: string;

  /**
   * 商品信息
   */
  products: StockAdjustProductVo[];
}
