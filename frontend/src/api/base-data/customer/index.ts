import { defHttp } from '/@/utils/http/axios';
import { PageResult } from '@/api/model/pageResult';
import { ContentTypeEnum, ResponseEnum } from '@/enums/httpEnum';
import { QueryCustomerSelectorVo } from '@/api/base-data/customer/model/queryCustomerSelectorVo';
import { UpdateCustomerVo } from '@/api/base-data/customer/model/updateCustomerVo';
import { CreateCustomerVo } from '@/api/base-data/customer/model/createCustomerVo';
import { GetCustomerBo } from '@/api/base-data/customer/model/getCustomerBo';
import { QueryCustomerVo } from '@/api/base-data/customer/model/queryCustomerVo';
import { QueryCustomerBo } from '@/api/base-data/customer/model/queryCustomerBo';
import { SelectorBo } from '@/api/common/SelectorBo';
import { CustomerSelectorBo } from '@/api/base-data/customer/model/customerSelectorBo';

const baseUrl = '/basedata/customer';
const selectorBaseUrl = '/selector';
const region = 'cloud-api';

export function selector(params: QueryCustomerSelectorVo): Promise<PageResult<CustomerSelectorBo>> {
  return defHttp.get<PageResult<CustomerSelectorBo>>(
    {
      url: selectorBaseUrl + '/customer',
      params,
    },
    {
      region,
    },
  );
}

export function loadCustomer(ids: string[]): Promise<SelectorBo[]> {
  return defHttp.post<SelectorBo[]>(
    {
      url: selectorBaseUrl + '/customer/load',
      data: ids,
    },
    {
      contentType: ContentTypeEnum.JSON,
      region,
    },
  );
}

/**
 * 查询列表
 */
export function query(params: QueryCustomerVo): Promise<PageResult<QueryCustomerBo>> {
  return defHttp.get<PageResult<QueryCustomerBo>>(
    {
      url: baseUrl + '/query',
      params,
    },
    {
      region,
    },
  );
}

/**
 * 根据ID查询
 * @param id
 */
export function get(id: string): Promise<GetCustomerBo> {
  return defHttp.get<GetCustomerBo>(
    {
      url: baseUrl,
      params: {
        id: id,
      },
    },
    {
      region,
    },
  );
}

/**
 * 生成编号
 */
export function generateCode(): Promise<string> {
  return defHttp.get<string>(
    {
      url: baseUrl + '/generate/code',
    },
    {
      region,
    },
  );
}

/**
 * 新增
 * @param data
 */
export function create(data: CreateCustomerVo): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl,
      data,
    },
    {
      contentType: ContentTypeEnum.FORM_URLENCODED,
      region,
    },
  );
}

/**
 * 修改
 * @param data
 */
export function update(data: UpdateCustomerVo): Promise<void> {
  return defHttp.put<void>(
    {
      url: baseUrl,
      data,
    },
    {
      contentType: ContentTypeEnum.FORM_URLENCODED,
      region,
    },
  );
}

/**
 * 下载导入模板
 */
export function downloadImportTemplate(): Promise<void> {
  return defHttp.get<void>(
    {
      url: baseUrl + '/import/template',
    },
    {
      responseType: ResponseEnum.BLOB,
      region,
    },
  );
}

/**
 * 导入
 */
export function importExcel(data: { id: string; file: Blob }): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl + '/import',
      data,
    },
    {
      contentType: ContentTypeEnum.BLOB,
      region,
    },
  );
}

/**
 * 导出客户信息
 */
export function exportList(data: QueryCustomerVo): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl + '/export',
      data,
    },
    {
      contentType: ContentTypeEnum.FORM_URLENCODED,
      region,
    },
  );
}

/**
 * 根据ID删除
 * @param id
 */
export function deleteById(id: string, showError: boolean = false): Promise<void> {
  return defHttp.delete<void>(
    {
      url: baseUrl,
      data: {
        id,
      },
    },
    {
      hiddenError: !showError,
      contentType: ContentTypeEnum.FORM_URLENCODED,
      region,
    },
  );
}
