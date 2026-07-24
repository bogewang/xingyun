package com.lframework.xingyun.sc.excel.sale.out;

import com.github.pagehelper.PageInfo;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.sc.dto.sale.out.QuerySaleOutSheetDetailDto;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;

public class SaleOutSheetDetailExportTaskWorker implements
        ExportTaskWorker<QuerySaleOutSheetVo, QuerySaleOutSheetDetailDto, SaleOutSheetDetailExportModel> {

    @Override
    public QuerySaleOutSheetVo parseParams(String json) {
        return JsonUtil.parseObject(json, QuerySaleOutSheetVo.class);
    }

    @Override
    public PageResult<QuerySaleOutSheetDetailDto> getDataList(int pageIndex, int pageSize,
                                                              QuerySaleOutSheetVo params) {

        SaleOutSheetService saleOutSheetService = ApplicationUtil.getBean(SaleOutSheetService.class);
        PageResult<QuerySaleOutSheetDetailDto> result = saleOutSheetService.queryDetail(pageIndex, pageSize,
                params);
        if (CollectionUtil.isEmpty(result.getDatas())) {
            return PageResultUtil.convert(new PageInfo<>());
        }
        return result;
    }

    @Override
    public SaleOutSheetDetailExportModel exportData(QuerySaleOutSheetDetailDto data) {
        SaleOutSheetDetailExportModel model = new SaleOutSheetDetailExportModel(data);
        model.setInquiryProduct(SaleOutSheetDetailExportModel.formatInquiryProduct(data.getInquiryProduct()));
        return model;
    }

    @Override
    public Class<SaleOutSheetDetailExportModel> getModelClass() {
        return SaleOutSheetDetailExportModel.class;
    }
}
