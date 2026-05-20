package com.lframework.xingyun.sc.excel.sale.out;

import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetProductProfitDto;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;

public class SaleOutSheetProductProfitExportTaskWorker implements
    ExportTaskWorker<QuerySaleOutSheetVo, SaleOutSheetProductProfitDto, SaleOutSheetProductProfitExportModel> {

  @Override
  public QuerySaleOutSheetVo parseParams(String json) {
    return JsonUtil.parseObject(json, QuerySaleOutSheetVo.class);
  }

  @Override
  public PageResult<SaleOutSheetProductProfitDto> getDataList(int pageIndex, int pageSize,
      QuerySaleOutSheetVo params) {

    SaleOutSheetService saleOutSheetService = ApplicationUtil.getBean(SaleOutSheetService.class);

    return saleOutSheetService.queryProductProfit(pageIndex, pageSize, params);
  }

  @Override
  public SaleOutSheetProductProfitExportModel exportData(SaleOutSheetProductProfitDto data) {
    return new SaleOutSheetProductProfitExportModel(data);
  }

  @Override
  public Class<SaleOutSheetProductProfitExportModel> getModelClass() {
    return SaleOutSheetProductProfitExportModel.class;
  }
}
