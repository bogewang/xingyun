import { defHttp } from '/@/utils/http/axios';
import { PageResult } from '@/api/model/pageResult';
import { ContentTypeEnum, ResponseEnum } from '@/enums/httpEnum';
import { QuerySupplierSelectorVo } from '@/api/base-data/supplier/model/querySupplierSelectorVo';
import { SelectorBo } from '@/api/common/SelectorBo';
import { UpdateSupplierVo } from '@/api/base-data/supplier/model/updateSupplierVo';
import { CreateSupplierVo } from '@/api/base-data/supplier/model/createSupplierVo';
import { GetSupplierBo } from '@/api/base-data/supplier/model/getSupplierBo';
import { QuerySupplierVo } from '@/api/base-data/supplier/model/querySupplierVo';
import { QuerySupplierBo } from '@/api/base-data/supplier/model/querySupplierBo';
import { UpdateSupplierAvailableVo } from '@/api/base-data/supplier/model/updateSupplierAvailableVo';

const baseUrl = '/basedata/supplier';
const selectorBaseUrl = '/selector';
const region = 'cloud-api';

export function selector(params: QuerySupplierSelectorVo): Promise<PageResult<SelectorBo>> {
  return defHttp.get<PageResult<SelectorBo>>(
    {
      url: selectorBaseUrl + '/supplier',
      params,
    },
    {
      region,
    },
  );
}

export function loadSupplier(ids: string[]): Promise<SelectorBo[]> {
  return defHttp.post<SelectorBo[]>(
    {
      url: selectorBaseUrl + '/supplier/load',
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
export function query(params: QuerySupplierVo): Promise<PageResult<QuerySupplierBo>> {
  return defHttp.get<PageResult<QuerySupplierBo>>(
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
export function get(id: string): Promise<GetSupplierBo> {
  return defHttp.get<GetSupplierBo>(
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
export function create(data: CreateSupplierVo): Promise<void> {
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
export function update(data: UpdateSupplierVo): Promise<void> {
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
 * 批量更新供应商可用状态。
 * @param data 批量更新请求参数
 */
export function updateAvailable(data: UpdateSupplierAvailableVo): Promise<void> {
  return defHttp.put<void>(
    {
      url: baseUrl + '/available',
      data,
    },
    {
      contentType: ContentTypeEnum.JSON,
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
 * 导出
 */
export function exportList(data: QuerySupplierVo): Promise<void> {
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
