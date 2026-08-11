import { PageVo } from '@/api/model/pageVo';

export interface QueryCustomerSelectorVo extends PageVo {
  /**
   * 显示标签
   */
  label: string;

  /**
   * 备注
   */
  description?: string;

  /**
   * 是否按备注排序
   */
  orderByDescription?: boolean;
}
