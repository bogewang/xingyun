import { SortPageVo } from '@/api/model/sortPageVo';

export interface QueryCustomerVo extends SortPageVo {
  /**
   * 编号
   */
  code: string;

  /**
   * 名称
   */
  name: string;

  /**
   * 昵称
   */
  nickName: string;

  /**
   * 备注
   */
  description: string;
}
