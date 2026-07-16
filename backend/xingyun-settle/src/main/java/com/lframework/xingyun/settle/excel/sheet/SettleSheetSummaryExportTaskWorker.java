package com.lframework.xingyun.settle.excel.sheet;

import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.xingyun.settle.bo.sheet.SettleSheetSummaryBo;
import com.lframework.xingyun.settle.service.SettleSheetSummaryService;
import com.lframework.xingyun.settle.vo.sheet.QuerySettleSheetSummaryVo;

import java.util.Collections;
import java.util.List;

public class SettleSheetSummaryExportTaskWorker implements
    ExportTaskWorker<QuerySettleSheetSummaryVo, SettleSheetSummaryBo, SettleSheetSummaryExportModel> {

  @Override
  public QuerySettleSheetSummaryVo parseParams(String json) {
    return JsonUtil.parseObject(json, QuerySettleSheetSummaryVo.class);
  }

  @Override
  public PageResult<SettleSheetSummaryBo> getDataList(int pageIndex, int pageSize,
      QuerySettleSheetSummaryVo params) {

    SettleSheetSummaryService summaryService = ApplicationUtil.getBean(
        SettleSheetSummaryService.class);
    List<SettleSheetSummaryBo> allData = summaryService.query(params);

    int safePageSize = pageSize <= 0 ? (allData.isEmpty() ? 1 : allData.size()) : pageSize;
    int safePageIndex = Math.max(pageIndex, 1);
    int fromIndex = Math.min((safePageIndex - 1) * safePageSize, allData.size());
    int toIndex = Math.min(fromIndex + safePageSize, allData.size());
    List<SettleSheetSummaryBo> pageData =
        fromIndex >= toIndex ? Collections.emptyList() : allData.subList(fromIndex, toIndex);

    PageResult<SettleSheetSummaryBo> pageResult = new PageResult<>();
    pageResult.setPageIndex(safePageIndex);
    pageResult.setPageSize(safePageSize);
    pageResult.setTotalCount(allData.size());
    pageResult.setTotalPage((int) Math.ceil(allData.size() / (double) safePageSize));
    pageResult.setHasPrev(safePageIndex > 1);
    pageResult.setHasNext(toIndex < allData.size());
    pageResult.setDatas(pageData);
    return pageResult;
  }

  @Override
  public SettleSheetSummaryExportModel exportData(SettleSheetSummaryBo data) {
    return new SettleSheetSummaryExportModel(data);
  }

  @Override
  public Class<SettleSheetSummaryExportModel> getModelClass() {
    return SettleSheetSummaryExportModel.class;
  }
}
