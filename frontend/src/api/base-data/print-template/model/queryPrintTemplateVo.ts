import { SortPageVo } from '@/api/model/sortPageVo';

export interface QueryPrintTemplateVo extends SortPageVo {
  /**
   * 名称
   */
  name: string;
  /**
   * 业务类型
   */
  bizType: string;
}
