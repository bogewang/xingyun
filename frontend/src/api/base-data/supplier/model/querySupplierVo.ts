import { SortPageVo } from '@/api/model/sortPageVo';

export interface QuerySupplierVo extends SortPageVo {
  /**
   *
   */
  serialVersionUID: long;

  /**
   * 编号
   */
  code: string;

  /**
   * 名称
   */
  name: string;

  /**
   * 状态；true 表示启用，false 表示停用
   */
  available: boolean | '';
}
