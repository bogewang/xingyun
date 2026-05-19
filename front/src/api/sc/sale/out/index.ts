import { defHttp } from '/@/utils/http/axios';
import { ContentTypeEnum, ResponseEnum } from '@/enums/httpEnum';
import { PageResult } from '@/api/model/pageResult';
import { GetPaymentDateBo } from '@/api/sc/purchase/receive/model/getPaymentDateBo';
import { QuerySaleOutSheetBo } from '@/api/sc/sale/out/model/querySaleOutSheetBo';
import { QuerySaleOutSheetDetailBo } from '@/api/sc/sale/out/model/querySaleOutSheetDetailBo';
import { QuerySaleOutSheetVo } from '@/api/sc/sale/out/model/querySaleOutSheetVo';
import { SaleOutSheetProfitSummaryBo } from '@/api/sc/sale/out/model/saleOutSheetProfitSummaryBo';
import { GetSaleOutSheetBo } from '@/api/sc/sale/out/model/getSaleOutSheetBo';
import { SaleOutSheetWithReturnBo } from '@/api/sc/sale/out/model/saleOutSheetWithReturnBo';
import { QuerySaleOutSheetWithReturnVo } from '@/api/sc/sale/out/model/querySaleOutSheetWithReturnVo';
import { QuerySaleOutSheetWithReturnBo } from '@/api/sc/sale/out/model/querySaleOutSheetWithReturnBo';
import { CreateSaleOutSheetVo } from '@/api/sc/sale/out/model/createSaleOutSheetVo';
import { UpdateSaleOutSheetVo } from '@/api/sc/sale/out/model/updateSaleOutSheetVo';
import { ApprovePassSaleOutSheetVo } from '@/api/sc/sale/out/model/approvePassSaleOutSheetVo';
import { ApproveRefuseSaleOutSheetVo } from '@/api/sc/sale/out/model/approveRefuseSaleOutSheetVo';
import { PrintSaleOrderBo } from '@/api/sc/sale/order/model/printSaleOrderBo';
import { PrintSaleTagBo } from '@/api/sc/sale/order/model/PrintSaleTagBo';

const baseUrl = '/sale/out/sheet';
const region = 'cloud-api';

type TagPrintParams = QuerySaleOutSheetVo & {
  idList?: string[];
};

/**
 * 打印
 */
export function print(id: string): Promise<PrintSaleOrderBo> {
  return defHttp.get<PrintSaleOrderBo>(
    {
      url: baseUrl + '/print',
      params: {
        id,
      },
    },
    {
      region,
    },
  );
}

/**
 * 订单列表
 */
export function query(params: QuerySaleOutSheetVo): Promise<PageResult<QuerySaleOutSheetBo>> {
  return defHttp.get<PageResult<QuerySaleOutSheetBo>>(
    {
      url: baseUrl + '/query',
      params,
    },
    {
      region,
    },
  );
}

export function queryDetail(
  params: QuerySaleOutSheetVo,
): Promise<PageResult<QuerySaleOutSheetDetailBo>> {
  return defHttp.get<PageResult<QuerySaleOutSheetDetailBo>>(
    {
      url: baseUrl + '/query/detail',
      params,
    },
    {
      region,
    },
  );
}

/**
 * 销售利润汇总
 */
export function queryProfitSummary(
  params: QuerySaleOutSheetVo,
): Promise<SaleOutSheetProfitSummaryBo> {
  return defHttp.get<SaleOutSheetProfitSummaryBo>(
    {
      url: baseUrl + '/profit/summary',
      params,
    },
    {
      region,
    },
  );
}

/**
 * 销售利润列表
 */
export function queryProfit(params: QuerySaleOutSheetVo): Promise<PageResult<QuerySaleOutSheetBo>> {
  return defHttp.get<PageResult<QuerySaleOutSheetBo>>(
    {
      url: baseUrl + '/profit/query',
      params,
    },
    {
      region,
    },
  );
}

/**
 * 标签打印
 */
export function tagPrint(params: TagPrintParams): Promise<PrintSaleTagBo[]> {
  return defHttp.post<PrintSaleTagBo[]>(
    {
      url: baseUrl + '/tagPrint',
      data: params,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/**
 * 买菜汇总导出
 */
export function exportMarketBuySummary(params: QuerySaleOutSheetVo): Promise<void> {
  return defHttp.get<void>(
    {
      url: baseUrl + '/export/marketBuySummary',
      params,
    },
    {
      region,
      responseType: ResponseEnum.BLOB,
    },
  );
}

/**
 * 导出
 */
export function exportList(data: QuerySaleOutSheetVo): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl + '/export',
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.FORM_URLENCODED,
    },
  );
}

/**
 * 导出订单详情
 */
export function exportDetail(data: QuerySaleOutSheetVo): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl + '/exportDetail',
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/**
 * 销售利润（按单据）导出
 */
export function exportProfit(data: QuerySaleOutSheetVo): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl + '/profit/export',
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.FORM_URLENCODED,
    },
  );
}

/**
 * 销售导出
 */
export function exportSales(data: QuerySaleOutSheetVo): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl + '/export/sales',
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
      responseType: ResponseEnum.BLOB,
    },
  );
}

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
 * 下载销售出库查询导入模板
 */
export function downloadQueryImportTemplate(): Promise<void> {
  return defHttp.get<void>(
    {
      url: baseUrl + '/import/query/template',
    },
    {
      responseType: ResponseEnum.BLOB,
      region,
    },
  );
}

/**
 * 销售出库查询页面导入并创建订单
 */
export function importByQuery(data: { file: Blob }): Promise<string[]> {
  return defHttp.post<string[]>(
    {
      url: baseUrl + '/import/query',
      data,
    },
    {
      contentType: ContentTypeEnum.BLOB,
      region,
    },
  );
}

/**
 * 查询详情
 */
export function get(id: string): Promise<GetSaleOutSheetBo> {
  return defHttp.get<GetSaleOutSheetBo>(
    {
      url: baseUrl,
      params: {
        id,
      },
    },
    {
      region,
    },
  );
}

/**
 * 根据客户ID查询默认付款日期
 */
export function getPaymentDate(customerId: string): Promise<GetPaymentDateBo> {
  return defHttp.get<GetPaymentDateBo>(
    {
      url: baseUrl + '/paymentdate',
      params: {
        customerId,
      },
    },
    {
      region,
    },
  );
}

/**
 * 根据ID查询（销售退货业务）
 */
export function getWithReturn(id: string): Promise<SaleOutSheetWithReturnBo> {
  return defHttp.get<SaleOutSheetWithReturnBo>(
    {
      url: baseUrl + '/return',
      params: {
        id,
      },
    },
    {
      region,
    },
  );
}

/**
 * 查询列表（销售退货业务）
 */
export function queryWithReturn(
  params: QuerySaleOutSheetWithReturnVo,
): Promise<PageResult<QuerySaleOutSheetWithReturnBo>> {
  return defHttp.get<PageResult<QuerySaleOutSheetWithReturnBo>>(
    {
      url: baseUrl + '/query/return',
      params,
    },
    {
      region,
    },
  );
}

/**
 * 加载列表（销售退货业务）
 */
export function loadWithReturn(ids: string[]): Promise<QuerySaleOutSheetWithReturnBo[]> {
  return defHttp.post<QuerySaleOutSheetWithReturnBo[]>(
    {
      url: baseUrl + '/query/return/load',
      data: ids,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/**
 * 新增
 */
export function create(data: CreateSaleOutSheetVo): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl,
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/**
 * 修改
 */
export function update(data: UpdateSaleOutSheetVo): Promise<void> {
  return defHttp.put<void>(
    {
      url: baseUrl,
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/**
 * 审核通过
 */
export function approvePass(data: ApprovePassSaleOutSheetVo): Promise<void> {
  return defHttp.patch<void>(
    {
      url: baseUrl + '/approve/pass',
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/**
 * 批量审核通过
 */
export function batchApprovePass(
  data: ApprovePassSaleOutSheetVo,
  showError: boolean = false,
): Promise<void> {
  return defHttp.patch<void>(
    {
      url: baseUrl + '/approve/pass',
      data,
    },
    {
      hiddenError: !showError,
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/**
 * 直接审核通过
 */
export function directApprovePass(data: CreateSaleOutSheetVo): Promise<void> {
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

/**
 * 审核拒绝
 */
export function approveRefuse(data: ApproveRefuseSaleOutSheetVo): Promise<void> {
  return defHttp.patch<void>(
    {
      url: baseUrl + '/approve/refuse',
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/**
 * 批量审核拒绝
 */
export function batchApproveRefuse(
  data: ApproveRefuseSaleOutSheetVo,
  showError: boolean = false,
): Promise<void> {
  return defHttp.patch<void>(
    {
      url: baseUrl + '/approve/refuse',
      data,
    },
    {
      hiddenError: !showError,
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/**
 * 删除
 */
export function deleteById(id: string): Promise<void> {
  return defHttp.delete<void>(
    {
      url: baseUrl,
      data: {
        id,
      },
    },
    {
      region,
      contentType: ContentTypeEnum.FORM_URLENCODED,
    },
  );
}

/**
 * 批量删除
 */
export function batchDelete(id: string, showError: boolean = false): Promise<void> {
  return defHttp.delete<void>(
    {
      url: baseUrl,
      data: {
        id,
      },
    },
    {
      hiddenError: !showError,
      region,
      contentType: ContentTypeEnum.FORM_URLENCODED,
    },
  );
}
