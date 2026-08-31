import { defHttp } from '/@/utils/http/axios';
import { PageResult } from '@/api/model/pageResult';
import { ContentTypeEnum, ResponseEnum } from '@/enums/httpEnum';
import { QueryQuoteSheetVo, QuoteSheetBo, QuoteSheetVo } from './model/quoteSheet';

const baseUrl = '/basedata/quote';
const region = 'cloud-api';

/** 查询报价单列表。 */
export function query(data: QueryQuoteSheetVo): Promise<PageResult<QuoteSheetBo>> {
  return defHttp.post<PageResult<QuoteSheetBo>>(
    { url: `${baseUrl}/query`, data },
    { contentType: ContentTypeEnum.JSON, region },
  );
}
/** 获取报价单详情。 */
export function get(id: string): Promise<QuoteSheetBo> {
  return defHttp.post<QuoteSheetBo>(
    { url: `${baseUrl}/get`, data: { id } },
    { contentType: ContentTypeEnum.FORM_URLENCODED, region },
  );
}
/** 新增报价单。 */
export function create(data: QuoteSheetVo): Promise<string> {
  return defHttp.post<string>(
    { url: `${baseUrl}/create`, data },
    { contentType: ContentTypeEnum.JSON, region },
  );
}
/** 修改报价单。 */
export function update(data: QuoteSheetVo): Promise<void> {
  return defHttp.post<void>(
    { url: `${baseUrl}/update`, data },
    { contentType: ContentTypeEnum.JSON, region },
  );
}
/** 删除报价单。 */
export function deleteById(id: string): Promise<void> {
  return defHttp.post<void>(
    { url: `${baseUrl}/delete`, data: { id } },
    { contentType: ContentTypeEnum.FORM_URLENCODED, region },
  );
}
/** 启用报价单。 */
export function enable(id: string): Promise<void> {
  return defHttp.post<void>(
    { url: `${baseUrl}/enable`, data: { id } },
    { contentType: ContentTypeEnum.FORM_URLENCODED, region },
  );
}
/** 停用报价单。 */
export function disable(id: string): Promise<void> {
  return defHttp.post<void>(
    { url: `${baseUrl}/disable`, data: { id } },
    { contentType: ContentTypeEnum.FORM_URLENCODED, region },
  );
}
/** 创建报价单商品明细导出任务。 */
export function exportDetail(data: QueryQuoteSheetVo): Promise<void> {
  return defHttp.post<void>(
    { url: `${baseUrl}/exportDetail`, data },
    { contentType: ContentTypeEnum.JSON, region },
  );
}
export function downloadImportTemplate(): Promise<void> {
  return defHttp.get<void>({ url: `${baseUrl}/import/template` }, { responseType: ResponseEnum.BLOB, region });
}
export function importExcel(data: { file: Blob }): Promise<any[]> { return defHttp.post<any[]>({ url: `${baseUrl}/import`, data }, { contentType: ContentTypeEnum.BLOB, region }); }
