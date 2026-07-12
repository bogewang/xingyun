import {defHttp} from '/@/utils/http/axios';
import {PageResult} from '@/api/model/pageResult';
import {SortPageVo} from '@/api/model/sortPageVo';
import {ContentTypeEnum, ResponseEnum} from '@/enums/httpEnum';

const url = '/basedata/unit';
const option = {contentType: ContentTypeEnum.FORM_URLENCODED, region: 'cloud-api'};

export function query(params: SortPageVo & {
  code?: string;
  name?: string
}): Promise<PageResult<any>> {
  return defHttp.get<PageResult<any>>({url: url + '/query', params}, {region: 'cloud-api'});
}

export const generateCode = () =>
  defHttp.get({url: url + '/generate/code'}, {region: 'cloud-api'});
export const create = (data: any) => defHttp.post({url, data}, option);
export const update = (data: any) => defHttp.put({url, data}, option);
export const remove = (id: string) => defHttp.delete({url, data: {id}}, option);
export const downloadImportTemplate = () =>
  defHttp.get(
    {url: url + '/import/template'},
    {responseType: ResponseEnum.BLOB, region: 'cloud-api'},
  );
export const importExcel = (data: { file: Blob }) =>
  defHttp.post(
    {url: url + '/import', data},
    {contentType: ContentTypeEnum.BLOB, region: 'cloud-api'},
  );
