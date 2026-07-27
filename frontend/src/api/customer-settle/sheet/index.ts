import { defHttp } from '/@/utils/http/axios';
import { ContentTypeEnum } from '@/enums/httpEnum';
import { PageResult } from '@/api/model/pageResult';
import { QueryCustomerSettleSheetVo } from '@/api/customer-settle/sheet/model/queryCustomerSettleSheetVo';
import { QueryCustomerSettleSheetBo } from '@/api/customer-settle/sheet/model/queryCustomerSettleSheetBo';
import { GetCustomerSettleSheetBo } from '@/api/customer-settle/sheet/model/getCustomerSettleSheetBo';
import { CreateCustomerSettleSheetVo } from '@/api/customer-settle/sheet/model/createCustomerSettleSheetVo';
import { QueryCustomerSaleSettleInfoVo } from '@/api/customer-settle/sheet/model/queryCustomerSaleSettleInfoVo';
import { CustomerSaleSettleInfoBo } from '@/api/customer-settle/sheet/model/customerSaleSettleInfoBo';
import { CustomerSettleOverviewBo } from '@/api/customer-settle/sheet/model/customerSettleOverviewBo';
import { QueryCustomerSettleOverviewVo } from '@/api/customer-settle/sheet/model/queryCustomerSettleOverviewVo';

const baseUrl = '/customer/settle/sheet';
const region = 'cloud-api';

/** 查询客户结算总览。 */
export function querySettleOverviews(
  data: QueryCustomerSettleOverviewVo,
): Promise<PageResult<CustomerSettleOverviewBo>> {
  return defHttp.post<PageResult<CustomerSettleOverviewBo>>(
    {
      url: baseUrl + '/settle-overviews',
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/** 导出客户结算总览。 */
export function exportSettleOverviews(data: QueryCustomerSettleOverviewVo): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl + '/export-settle-overviews',
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/** 查询客户销售结算工作台。 */
export function querySaleSettleInfos(
  data: QueryCustomerSaleSettleInfoVo,
): Promise<PageResult<CustomerSaleSettleInfoBo>> {
  return defHttp.post<PageResult<CustomerSaleSettleInfoBo>>(
    {
      url: baseUrl + '/sale-settle-infos',
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/** 导出客户销售结算工作台。 */
export function exportSaleSettleInfos(data: QueryCustomerSaleSettleInfoVo): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl + '/export-sale-settle-infos',
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/** 查询客户结算记录。 */
export function query(
  params: QueryCustomerSettleSheetVo,
): Promise<PageResult<QueryCustomerSettleSheetBo>> {
  return defHttp.get<PageResult<QueryCustomerSettleSheetBo>>(
    {
      url: baseUrl + '/query',
      params,
    },
    {
      region,
    },
  );
}

/** 导出客户结算记录。 */
export function exportRecord(data: QueryCustomerSettleSheetVo): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl + '/export-record',
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/** 查询客户结算记录详情。 */
export function get(id: string): Promise<GetCustomerSettleSheetBo> {
  return defHttp.get<GetCustomerSettleSheetBo>(
    {
      url: baseUrl,
      params: { id },
    },
    {
      region,
    },
  );
}

/** 直接审核通过客户结算单。 */
export function directApprovePass(data: CreateCustomerSettleSheetVo): Promise<void> {
  return defHttp.post<void>(
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
