import { SortPageVo } from '@/api/model/sortPageVo';

export interface QueryUnitVo extends SortPageVo {
  /**
   * 编码
   */
  code?: string;

  /**
   * 名称
   */
  name?: string;
}
