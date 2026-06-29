import { SortPageVo } from '@/api/model/sortPageVo';

export interface QueryProductStockVo extends SortPageVo {
  /**
   * 仓库ID
   */
  scId: string;

  /**
   * 商品编号
   */
  productCode: string;

  /**
   * 商品名称
   */
  productName: string;

  /**
   * 商品分类ID
   */
  categoryId: string;

  /**
   * 商品品牌ID
   */
  brandId: string;

  /**
   * 库存数量最小值
   */
  stockNumStart: number;

  /**
   * 库存数量最大值
   */
  stockNumEnd: number;
}
