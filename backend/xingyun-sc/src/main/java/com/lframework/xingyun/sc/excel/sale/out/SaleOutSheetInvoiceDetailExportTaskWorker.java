package com.lframework.xingyun.sc.excel.sale.out;

import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;

import java.util.Collections;
import java.util.List;

/**
 * 销售出库开票明细导出任务。
 */
public class SaleOutSheetInvoiceDetailExportTaskWorker implements ExportTaskWorker<QuerySaleOutSheetVo,
        SaleOutSheetInvoiceDetailExportModel, SaleOutSheetInvoiceDetailExportModel> {

    private List<SaleOutSheetInvoiceDetailExportModel> invoiceDetails;

    /**
     * 解析导出任务参数。
     *
     * @param json 参数 JSON
     * @return 查询参数
     */
    @Override
    public QuerySaleOutSheetVo parseParams(String json) {
        return JsonUtil.parseObject(json, QuerySaleOutSheetVo.class);
    }

    /**
     * 分页获取已按商品和单位汇总的开票明细。
     * <p>
     * 同一导出任务只加载一次汇总结果，避免每一页重复查询和聚合。
     *
     * @param pageIndex 页码
     * @param pageSize 每页数量
     * @param params 查询参数
     * @return 开票明细分页数据
     */
    @Override
    public PageResult<SaleOutSheetInvoiceDetailExportModel> getDataList(int pageIndex, int pageSize,
            QuerySaleOutSheetVo params) {
        List<SaleOutSheetInvoiceDetailExportModel> datas = loadInvoiceDetails(params);
        int fromIndex = Math.min((pageIndex - 1) * pageSize, datas.size());
        int toIndex = Math.min(fromIndex + pageSize, datas.size());
        return PageResultUtil.newInstance(pageIndex, pageSize, datas.size(), datas.subList(fromIndex, toIndex));
    }

    /**
     * 加载并缓存当前导出任务的开票明细。
     *
     * @param params 查询参数
     * @return 汇总后的开票明细
     */
    private List<SaleOutSheetInvoiceDetailExportModel> loadInvoiceDetails(QuerySaleOutSheetVo params) {
        if (invoiceDetails != null) {
            return invoiceDetails;
        }
        SaleOutSheetService saleOutSheetService = ApplicationUtil.getBean(SaleOutSheetService.class);
        invoiceDetails = saleOutSheetService.queryInvoiceDetail(params);
        if (invoiceDetails == null) {
            invoiceDetails = Collections.emptyList();
        }
        return invoiceDetails;
    }

    /**
     * 转换导出行。
     *
     * @param data 开票明细
     * @return 导出行
     */
    @Override
    public SaleOutSheetInvoiceDetailExportModel exportData(SaleOutSheetInvoiceDetailExportModel data) {
        return data;
    }

    /**
     * 获取导出模型类型。
     *
     * @return 导出模型类型
     */
    @Override
    public Class<SaleOutSheetInvoiceDetailExportModel> getModelClass() {
        return SaleOutSheetInvoiceDetailExportModel.class;
    }
}
