package com.lframework.xingyun.sc.excel.sale;

import com.github.pagehelper.PageInfo;
import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.sc.entity.SaleOrder;
import com.lframework.xingyun.sc.entity.SaleOrderDetail;
import com.lframework.xingyun.sc.service.sale.SaleOrderDetailService;
import com.lframework.xingyun.sc.service.sale.SaleOrderService;
import com.lframework.xingyun.sc.vo.sale.QuerySaleOrderVo;

import java.util.List;
import java.util.stream.Collectors;

public class SaleOrderDetailExportTaskWorker implements ExportTaskWorker<QuerySaleOrderVo, SaleOrderDetail, SaleOrderDetailExportModel> {

  @Override
  public QuerySaleOrderVo parseParams(String json) {
    return JsonUtil.parseObject(json, QuerySaleOrderVo.class);
  }

  @Override
  public PageResult<SaleOrderDetail> getDataList(int pageIndex, int pageSize, QuerySaleOrderVo params) {

    SaleOrderService saleOrderService = ApplicationUtil.getBean(SaleOrderService.class);
    PageResult<SaleOrder> result = saleOrderService.query(pageIndex, pageSize, params);
    List<String> orderIds = result.getDatas().stream().map(SaleOrder::getId).collect(Collectors.toList());
    // 根据订单ID查询订单详情
    SaleOrderDetailService saleOrderDetailService = ApplicationUtil.getBean(SaleOrderDetailService.class);
    List<SaleOrderDetail> details = saleOrderDetailService.getByOrderIds(orderIds);
    // todo 修改为直接返回 SaleOrderDetailExportModel
    return PageResultUtil.convert(new PageInfo<>(details));
  }

  @Override
  public SaleOrderDetailExportModel exportData(SaleOrderDetail data) {
    return new SaleOrderDetailExportModel(data);
  }

  @Override
  public Class<SaleOrderDetailExportModel> getModelClass() {
    return SaleOrderDetailExportModel.class;
  }
}
