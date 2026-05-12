import { PageVo } from '@/api/model/pageVo';

export interface QueryCustomerSelectorVo extends PageVo {
  /**
   * 显示标签
   */
  label: string;
}
