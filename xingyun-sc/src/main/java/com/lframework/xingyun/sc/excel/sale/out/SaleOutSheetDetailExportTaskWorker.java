package com.lframework.xingyun.sc.excel.sale.out;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetDetailService;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;

import java.util.List;
import java.util.stream.Collectors;

public class SaleOutSheetDetailExportTaskWorker implements
    ExportTaskWorker<QuerySaleOutSheetVo, SaleOutSheetDetail, SaleOutSheetDetailExportModel> {

  @Override
  public QuerySaleOutSheetVo parseParams(String json) {
    return JsonUtil.parseObject(json, QuerySaleOutSheetVo.class);
  }

  @Override
  public PageResult<SaleOutSheetDetail> getDataList(int pageIndex, int pageSize,
      QuerySaleOutSheetVo params) {

    SaleOutSheetService saleOutSheetService = ApplicationUtil.getBean(SaleOutSheetService.class);
    PageResult<SaleOutSheet> result = saleOutSheetService.query(pageIndex, pageSize, params);
    List<String> sheetIds = result.getDatas().stream().map(SaleOutSheet::getId).collect(Collectors.toList());
    if (CollectionUtil.isEmpty(sheetIds)) {
      return PageResultUtil.convert(new PageInfo<>());
    }

    SaleOutSheetDetailService saleOutSheetDetailService = ApplicationUtil.getBean(
        SaleOutSheetDetailService.class);
    List<SaleOutSheetDetail> details = saleOutSheetDetailService.list(
        Wrappers.lambdaQuery(SaleOutSheetDetail.class).in(SaleOutSheetDetail::getSheetId, sheetIds));

    return PageResultUtil.convert(new PageInfo<>(details));
  }

  @Override
  public SaleOutSheetDetailExportModel exportData(SaleOutSheetDetail data) {
    return new SaleOutSheetDetailExportModel(data);
  }

  @Override
  public Class<SaleOutSheetDetailExportModel> getModelClass() {
    return SaleOutSheetDetailExportModel.class;
  }
}
