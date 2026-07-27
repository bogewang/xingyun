import { defHttp } from '/@/utils/http/axios';
import { ContentTypeEnum } from '@/enums/httpEnum';
import { CreateCustomerSettleCheckSheetVo } from '@/api/customer-settle/check/model/createCustomerSettleCheckSheetVo';

const baseUrl = '/customer/settle/checksheet';
const region = 'cloud-api';

/**
 * 直接确认客户对账单（创建并审核通过）。
 */
export function directApprovePass(data: CreateCustomerSettleCheckSheetVo): Promise<string> {
  return defHttp.post<string>(
    {
      url: baseUrl + '/approve/pass/direct',
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}
