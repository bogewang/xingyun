package com.lframework.xingyun.settle.excel.sheet;

import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.sc.entity.ReceiveSheet;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetService;
import com.lframework.xingyun.sc.vo.purchase.receive.QueryReceiveSheetVo;
import com.lframework.xingyun.settle.bo.sheet.ReceiveSheetSettleInfoBo;
import com.lframework.xingyun.settle.service.SettleSheetService;

import java.util.List;

public class ReceiveSheetSettleInfoExportTaskWorker implements
    ExportTaskWorker<QueryReceiveSheetVo, ReceiveSheetSettleInfoBo, ReceiveSheetSettleInfoExportModel> {

  @Override
  public QueryReceiveSheetVo parseParams(String json) {
    return JsonUtil.parseObject(json, QueryReceiveSheetVo.class);
  }

  @Override
  public PageResult<ReceiveSheetSettleInfoBo> getDataList(int pageIndex, int pageSize,
      QueryReceiveSheetVo params) {

    ReceiveSheetService receiveSheetService = ApplicationUtil.getBean(ReceiveSheetService.class);
    SettleSheetService settleSheetService = ApplicationUtil.getBean(SettleSheetService.class);

    PageResult<ReceiveSheet> pageResult = receiveSheetService.query(pageIndex, pageSize, params);
    List<ReceiveSheetSettleInfoBo> results = settleSheetService.queryReceiveSheetSettleInfos(
        pageResult.getDatas());

    return PageResultUtil.rebuild(pageResult, results);
  }

  @Override
  public ReceiveSheetSettleInfoExportModel exportData(ReceiveSheetSettleInfoBo data) {
    return new ReceiveSheetSettleInfoExportModel(data);
  }

  @Override
  public Class<ReceiveSheetSettleInfoExportModel> getModelClass() {
    return ReceiveSheetSettleInfoExportModel.class;
  }
}
