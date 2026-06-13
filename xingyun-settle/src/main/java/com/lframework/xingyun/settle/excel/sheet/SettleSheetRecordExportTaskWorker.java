package com.lframework.xingyun.settle.excel.sheet;

import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.xingyun.settle.entity.SettleSheet;
import com.lframework.xingyun.settle.service.SettleSheetService;
import com.lframework.xingyun.settle.vo.sheet.QuerySettleSheetVo;

public class SettleSheetRecordExportTaskWorker implements
    ExportTaskWorker<QuerySettleSheetVo, SettleSheet, SettleSheetRecordExportModel> {

  @Override
  public QuerySettleSheetVo parseParams(String json) {
    return JsonUtil.parseObject(json, QuerySettleSheetVo.class);
  }

  @Override
  public PageResult<SettleSheet> getDataList(int pageIndex, int pageSize, QuerySettleSheetVo params) {
    SettleSheetService settleSheetService = ApplicationUtil.getBean(SettleSheetService.class);
    return settleSheetService.query(pageIndex, pageSize, params);
  }

  @Override
  public SettleSheetRecordExportModel exportData(SettleSheet data) {
    return new SettleSheetRecordExportModel(data);
  }

  @Override
  public Class<SettleSheetRecordExportModel> getModelClass() {
    return SettleSheetRecordExportModel.class;
  }
}
