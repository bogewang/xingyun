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
   * 采购价
   */
  purchasePrice: number;

  /**
   * 零售价
   */
  retailPrice: number;

  /**
   * 销售价
   */
  salePrice: number;

  /**
   * 别名
   */
  alias: string;

  /**
   * 备注
   */
  remark: string;

  /**
   * 备注2
   */
  remark2: string;

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
