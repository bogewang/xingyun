package com.lframework.xingyun.sc.excel.purchase;

import com.github.pagehelper.PageInfo;
import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.sc.entity.PurchaseOrder;
import com.lframework.xingyun.sc.entity.PurchaseOrderDetail;
import com.lframework.xingyun.sc.service.purchase.PurchaseOrderDetailService;
import com.lframework.xingyun.sc.service.purchase.PurchaseOrderService;
import com.lframework.xingyun.sc.vo.purchase.QueryPurchaseOrderVo;

import java.util.List;
import java.util.stream.Collectors;

public class PurchaseOrderDetailExportTaskWorker implements ExportTaskWorker<QueryPurchaseOrderVo, PurchaseOrderDetail, PurchaseOrderDetailExportModel>{

  @Override
  public QueryPurchaseOrderVo parseParams(String json) {
    return JsonUtil.parseObject(json, QueryPurchaseOrderVo.class);
  }

  @Override
  public PageResult<PurchaseOrderDetail> getDataList(int pageIndex, int pageSize, QueryPurchaseOrderVo params) {
    PurchaseOrderService purchaseOrderService = ApplicationUtil.getBean(PurchaseOrderService.class);
    PageResult<PurchaseOrder> result = purchaseOrderService.query(pageIndex, pageSize, params);
    List<String> orderIds = result.getDatas().stream().map(PurchaseOrder::getId).collect(Collectors.toList());
    // 根据订单ID查询订单详情
    PurchaseOrderDetailService purchaseOrderDetailService = ApplicationUtil.getBean(PurchaseOrderDetailService.class);
    List<PurchaseOrderDetail> details = purchaseOrderDetailService.getByOrderIds(orderIds);
    // todo 修改为直接返回 PurchaseOrderDetailExportModel
    return PageResultUtil.convert(new PageInfo<>(details));
  }

  @Override
  public PurchaseOrderDetailExportModel exportData(PurchaseOrderDetail data) {
    return new PurchaseOrderDetailExportModel(data);
  }

  @Override
  public Class<PurchaseOrderDetailExportModel> getModelClass() {
    return PurchaseOrderDetailExportModel.class;
  }
}
