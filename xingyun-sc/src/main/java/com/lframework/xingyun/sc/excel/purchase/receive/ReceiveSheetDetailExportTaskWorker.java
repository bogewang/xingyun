package com.lframework.xingyun.sc.excel.purchase.receive;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.sc.entity.ReceiveSheet;
import com.lframework.xingyun.sc.entity.ReceiveSheetDetail;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetDetailService;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetService;
import com.lframework.xingyun.sc.vo.purchase.receive.QueryReceiveSheetVo;

import java.util.List;
import java.util.stream.Collectors;

public class ReceiveSheetDetailExportTaskWorker implements
    ExportTaskWorker<QueryReceiveSheetVo, ReceiveSheetDetail, ReceiveSheetDetailExportModel> {

  @Override
  public QueryReceiveSheetVo parseParams(String json) {
    return JsonUtil.parseObject(json, QueryReceiveSheetVo.class);
  }

  @Override
  public PageResult<ReceiveSheetDetail> getDataList(int pageIndex, int pageSize,
      QueryReceiveSheetVo params) {

    ReceiveSheetService receiveSheetService = ApplicationUtil.getBean(ReceiveSheetService.class);
    PageResult<ReceiveSheet> result = receiveSheetService.query(pageIndex, pageSize, params);
    List<String> sheetIds = result.getDatas().stream().map(ReceiveSheet::getId).collect(Collectors.toList());
    if (CollectionUtil.isEmpty(sheetIds)) {
      return PageResultUtil.convert(new PageInfo<>());
    }

    ReceiveSheetDetailService receiveSheetDetailService = ApplicationUtil.getBean(ReceiveSheetDetailService.class);
    List<ReceiveSheetDetail> details = receiveSheetDetailService.list(
        Wrappers.lambdaQuery(ReceiveSheetDetail.class).in(ReceiveSheetDetail::getSheetId, sheetIds));

    return PageResultUtil.convert(new PageInfo<>(details));
  }

  @Override
  public ReceiveSheetDetailExportModel exportData(ReceiveSheetDetail data) {
    return new ReceiveSheetDetailExportModel(data);
  }

  @Override
  public Class<ReceiveSheetDetailExportModel> getModelClass() {
    return ReceiveSheetDetailExportModel.class;
  }
}
