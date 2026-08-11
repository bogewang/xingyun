import { SelectorBo } from '@/api/common/SelectorBo';

export interface CustomerSelectorBo extends SelectorBo {
  /**
   * 客户备注
   */
  description: string;
}
