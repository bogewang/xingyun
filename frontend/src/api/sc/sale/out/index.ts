import { defHttp } from '/@/utils/http/axios';
import { ContentTypeEnum, ResponseEnum } from '@/enums/httpEnum';
import { PageResult } from '@/api/model/pageResult';
import { GetPaymentDateBo } from '@/api/sc/purchase/receive/model/getPaymentDateBo';
import { QuerySaleOutSheetBo } from '@/api/sc/sale/out/model/querySaleOutSheetBo';
import { QuerySaleOutSheetDetailBo } from '@/api/sc/sale/out/model/querySaleOutSheetDetailBo';
import { QuerySaleOutSheetVo } from '@/api/sc/sale/out/model/querySaleOutSheetVo';
import { SaleOutSheetProfitSummaryBo } from '@/api/sc/sale/out/model/saleOutSheetProfitSummaryBo';
import { SaleOutSheetProfitTrendBo } from '@/api/sc/sale/out/model/saleOutSheetProfitTrendBo';
import { SaleOutSheetProductProfitBo } from '@/api/sc/sale/out/model/saleOutSheetProductProfitBo';
import { SaleOutSheetProductProfitSummaryBo } from '@/api/sc/sale/out/model/saleOutSheetProductProfitSummaryBo';
import { SaleOutSheetProductProfitTrendBo } from '@/api/sc/sale/out/model/saleOutSheetProductProfitTrendBo';
import { GetSaleOutSheetBo } from '@/api/sc/sale/out/model/getSaleOutSheetBo';
import { SaleOutSheetWithReturnBo } from '@/api/sc/sale/out/model/saleOutSheetWithReturnBo';
import { QuerySaleOutSheetWithReturnVo } from '@/api/sc/sale/out/model/querySaleOutSheetWithReturnVo';
import { QuerySaleOutSheetWithReturnBo } from '@/api/sc/sale/out/model/querySaleOutSheetWithReturnBo';
import { CreateSaleOutSheetVo } from '@/api/sc/sale/out/model/createSaleOutSheetVo';
import { UpdateSaleOutSheetVo } from '@/api/sc/sale/out/model/updateSaleOutSheetVo';
import { UpdateSaleOutSheetDescriptionVo } from '@/api/sc/sale/out/model/updateSaleOutSheetDescriptionVo';
import { ApprovePassSaleOutSheetVo } from '@/api/sc/sale/out/model/approvePassSaleOutSheetVo';
import { ApproveRefuseSaleOutSheetVo } from '@/api/sc/sale/out/model/approveRefuseSaleOutSheetVo';
import { BatchUpdateSaleOutSheetPriceVo } from '@/api/sc/sale/out/model/batchUpdateSaleOutSheetPriceVo';
import { SyncInquirySalePriceVo } from '@/api/sc/sale/out/model/syncInquirySalePriceVo';
import { MergeSaleOutSheetVo } from '@/api/sc/sale/out/model/mergeSaleOutSheetVo';
import { PrintSaleOrderBo } from '@/api/sc/sale/order/model/printSaleOrderBo';
import { PrintSaleTagBo } from '@/api/sc/sale/order/model/PrintSaleTagBo';

const baseUrl = '/sale/out/sheet';
const region = 'cloud-api';

type TagPrintParams = QuerySaleOutSheetVo & {
  idList?: string[];
  detailIdList?: string[];
};

type MarketBuySummaryParams = {
  idList: string[];
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
      paramsSerializer: {
        indexes: null,
      },
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
      paramsSerializer: {
        indexes: null,
      },
    },
    {
      region,
    },
  );
}

export function queryPriceCheckDetail(
  params: QuerySaleOutSheetVo,
): Promise<PageResult<QuerySaleOutSheetDetailBo>> {
  return defHttp.get<PageResult<QuerySaleOutSheetDetailBo>>(
    {
      url: baseUrl + '/query/priceCheck',
      params,
    },
    {
      region,
    },
  );
}

export function getPriceUniqueConfig(): Promise<boolean> {
  return defHttp.get<boolean>(
    {
      url: baseUrl + '/price/unique/config',
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
 * 销售趋势
 */
export function queryProfitTrend(
  params: QuerySaleOutSheetVo,
): Promise<SaleOutSheetProfitTrendBo[]> {
  return defHttp.get<SaleOutSheetProfitTrendBo[]>(
    {
      url: baseUrl + '/profit/trend',
      params,
    },
    {
      region,
    },
  );
}

/**
 * 销售利润（按商品）汇总
 */
export function queryProductProfitSummary(
  params: QuerySaleOutSheetVo,
): Promise<SaleOutSheetProductProfitSummaryBo> {
  return defHttp.get<SaleOutSheetProductProfitSummaryBo>(
    {
      url: baseUrl + '/profit/product/summary',
      params,
    },
    {
      region,
    },
  );
}

/**
 * 销售利润（按商品）列表
 */
export function queryProductProfit(
  params: QuerySaleOutSheetVo,
): Promise<PageResult<SaleOutSheetProductProfitBo>> {
  return defHttp.get<PageResult<SaleOutSheetProductProfitBo>>(
    {
      url: baseUrl + '/profit/product/query',
      params,
    },
    {
      region,
    },
  );
}

/**
 * 销售利润（按商品）趋势
 */
export function queryProductProfitTrend(
  params: QuerySaleOutSheetVo,
): Promise<SaleOutSheetProductProfitTrendBo[]> {
  return defHttp.get<SaleOutSheetProductProfitTrendBo[]>(
    {
      url: baseUrl + '/profit/product/trend',
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
 * 获取标签打印分类缓存
 */
export function getTagPrintCategoryCache(): Promise<string[]> {
  return defHttp.get<string[]>(
    {
      url: baseUrl + '/tagPrint/category/cache',
    },
    {
      region,
    },
  );
}

/**
 * 保存标签打印分类缓存
 */
export function saveTagPrintCategoryCache(categoryIds: string[]): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl + '/tagPrint/category/cache',
      data: categoryIds,
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
export function exportMarketBuySummary(params: MarketBuySummaryParams): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl + '/export/marketBuySummary',
      data: params,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/**
 * 买菜汇总2导出
 */
export function exportMarketBuySummary2(params: MarketBuySummaryParams): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl + '/export/marketBuySummary2',
      data: params,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
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

export function exportDetailDailySummary(data: QuerySaleOutSheetVo): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl + '/exportDetail/dailySummary',
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
      responseType: ResponseEnum.BLOB,
    },
  );
}

/**
 * 导出开票明细。
 */
export function exportInvoiceDetail(data: QuerySaleOutSheetVo): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl + '/exportDetail/invoice',
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
 * 销售利润（按商品）导出
 */
export function exportProductProfit(data: QuerySaleOutSheetVo): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl + '/profit/product/export',
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
 * 成本重算
 */
export function refreshCostPrice(startDate: string, endDate: string): Promise<void> {
  return defHttp.post<void>(
    {
      url: baseUrl + '/refreshCostPrice',
      data: {
        startDate,
        endDate,
      },
    },
    {
      region,
      contentType: ContentTypeEnum.FORM_URLENCODED,
    },
  );
}

/**
 * 月底成本重算 - 月加权平均法
 */
export function monthEndRecalculate(params: {
  beginDate: string;
  endDate: string;
  scId?: string;
}): Promise<{
  updatedSheetCount: number;
  updatedDetailCount: number;
  notFilledCount: number;
}> {
  return defHttp.post<{
    updatedSheetCount: number;
    updatedDetailCount: number;
    notFilledCount: number;
  }>(
    {
      url: baseUrl + '/month-end/recalculate',
      data: params,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/**
 * 月底成本重算（启动）—— 计算并缓存全范围月加权均价
 * @returns taskId 及 totalDays
 */
export function startMonthEndRecalculate(params: {
  calcBeginDate: string;
  calcEndDate: string;
  scId?: string;
}): Promise<{
  taskId: string;
  totalDays: number;
}> {
  return defHttp.post<{
    taskId: string;
    totalDays: number;
  }>(
    {
      url: baseUrl + '/month-end/recalculate/start',
      data: params,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/**
 * 月底成本重算（逐天执行）—— 使用缓存的均价处理指定日期的单据
 * @returns 当天执行结果，hasError=true 时表示失败
 */
export function stepMonthEndRecalculate(params: { taskId: string; processDate: string }): Promise<{
  updatedSheetCount: number;
  updatedDetailCount: number;
  notFilledCount: number;
  processedDate: string;
  hasError: boolean;
  errorMsg?: string;
}> {
  return defHttp.post<{
    updatedSheetCount: number;
    updatedDetailCount: number;
    notFilledCount: number;
    processedDate: string;
    hasError: boolean;
    errorMsg?: string;
  }>(
    {
      url: baseUrl + '/month-end/recalculate/step',
      data: params,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
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
 * 合并订单
 */
export function merge(data: MergeSaleOutSheetVo): Promise<string> {
  return defHttp.patch<string>(
    {
      url: baseUrl + '/merge',
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/**
 * 修改备注
 */
export function updateDescription(data: UpdateSaleOutSheetDescriptionVo): Promise<void> {
  return defHttp.patch<void>(
    {
      url: baseUrl + '/description',
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/**
 * 批量调整售价
 */
export function batchUpdatePrice(data: BatchUpdateSaleOutSheetPriceVo): Promise<void> {
  return defHttp.patch<void>(
    {
      url: baseUrl + '/price',
      data,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/**
 * 批量送货
 */
export function batchDelivery(ids: string[]): Promise<void> {
  return defHttp.patch<void>(
    {
      url: baseUrl + '/delivery',
      data: { ids },
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}

/**
 * 按订单日期同步询价商品销售价
 */
export function syncInquirySalePrice(data: SyncInquirySalePriceVo): Promise<void> {
  return defHttp.patch<void>(
    {
      url: baseUrl + '/inquiry-price/sync',
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
