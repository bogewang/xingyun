export interface QueryProductBo {
  /**
   * ID
   */
  id: string;

  /**
   * 编号
   */
  code: string;

  /**
   * 名称
   */
  name: string;

  /**
   * 简称
   */
  shortName: string;

  /**
   * SKU
   */
  skuCode: string;

  /**
   * 单位
   */
  unit: string;

  /**
   * 规格
   */
  spec: string;

  /**
   * 库存数量
   */
  stockNum: number;

  /**
   * 采购价
   */
  purchasePrice: number;

  /**
   * 零售价
   */
  retailPrice: number;

  /**
   * 分类名称
   */
  categoryName: string;

  /**
   * 品牌名称
   */
  brandName: string;

  /**
   * 商品类型
   */
  productType: number;

  /**
   * 状态
   */
  available: boolean;

  /**
   * 创建时间
   */
  createTime: string;

  /**
   * 修改时间
   */
  updateTime: string;
}
