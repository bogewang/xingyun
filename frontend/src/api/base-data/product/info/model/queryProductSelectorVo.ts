import { PageVo } from '@/api/model/pageVo';

export interface QueryProductSelectorVo extends PageVo {
  /**
   * 报价单 ID，后端据此排除已有明细商品。
   */
  quoteSheetId?: string;

  /**
   * 关键字
   */
  condition: string;

  /**
   * 编号
   */
  code: string;

  /**
   * 名称
   */
  name: string;

  /**
   * SKU
   */
  skuCode: string;

  /**
   * 简称
   */
  shortName: string;

  /**
   * 品牌ID
   */
  brandId: string;

  /**
   * 分类ID
   */
  categoryId: string;

  /**
   * 创建起始时间
   */
  startTime: string;

  /**
   * 创建截止时间
   */
  endTime: string;

  /**
   * 商品类型
   */
  productType: number;

  /**
   * 状态
   */
  available: boolean;
}
