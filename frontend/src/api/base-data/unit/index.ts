import { defHttp } from '/@/utils/http/axios';
import { PageResult } from '@/api/model/pageResult';
import { ContentTypeEnum, ResponseEnum } from '@/enums/httpEnum';
import { QueryUnitVo } from './model/queryUnitVo';
import { QueryUnitBo } from './model/queryUnitBo';
import { CreateUnitVo } from './model/createUnitVo';
import { UpdateUnitVo } from './model/updateUnitVo';

const url = '/basedata/unit';
const option = { contentType: ContentTypeEnum.FORM_URLENCODED, region: 'cloud-api' };

export function query(params: QueryUnitVo): Promise<PageResult<QueryUnitBo>> {
  return defHttp.get<PageResult<QueryUnitBo>>(
    { url: url + '/query', params },
    { region: 'cloud-api' },
  );
}

export const generateCode = (): Promise<string> =>
  defHttp.get<string>({ url: url + '/generate/code' }, { region: 'cloud-api' });

export const create = (data: CreateUnitVo): Promise<void> =>
  defHttp.post<void>({ url, data }, option);

export const update = (data: UpdateUnitVo): Promise<void> =>
  defHttp.put<void>({ url, data }, option);

export const remove = (id: string): Promise<void> =>
  defHttp.delete<void>({ url, data: { id } }, option);

export const downloadImportTemplate = (): Promise<Blob> =>
  defHttp.get<Blob>(
    { url: url + '/import/template' },
    { responseType: ResponseEnum.BLOB, region: 'cloud-api' },
  );

export const importExcel = (data: { file: Blob }): Promise<void> =>
  defHttp.post<void>(
    { url: url + '/import', data },
    { contentType: ContentTypeEnum.BLOB, region: 'cloud-api' },
  );

/**
 * 导出
 */
export const exportList = (data: QueryUnitVo): Promise<void> =>
  defHttp.post<void>(
    { url: url + '/export', data },
    { contentType: ContentTypeEnum.FORM_URLENCODED, region: 'cloud-api' },
  );
