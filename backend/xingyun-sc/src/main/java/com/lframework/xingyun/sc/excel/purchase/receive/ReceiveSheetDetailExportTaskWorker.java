package com.lframework.xingyun.sc.excel.purchase.receive;

import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.sc.dto.purchase.receive.QueryReceiveSheetDetailDto;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetService;
import com.lframework.xingyun.sc.vo.purchase.receive.QueryReceiveSheetVo;

public class ReceiveSheetDetailExportTaskWorker implements
    ExportTaskWorker<QueryReceiveSheetVo, QueryReceiveSheetDetailDto, ReceiveSheetDetailExportModel> {

  @Override
  public QueryReceiveSheetVo parseParams(String json) {
    return JsonUtil.parseObject(json, QueryReceiveSheetVo.class);
  }

  @Override
  public PageResult<QueryReceiveSheetDetailDto> getDataList(int pageIndex, int pageSize,
      QueryReceiveSheetVo params) {

    ReceiveSheetService receiveSheetService = ApplicationUtil.getBean(ReceiveSheetService.class);
    return receiveSheetService.queryDetail(pageIndex, pageSize, params);
  }

  @Override
  public ReceiveSheetDetailExportModel exportData(QueryReceiveSheetDetailDto data) {
    ProductService productService = ApplicationUtil.getBean(ProductService.class);
    Product product = productService.findById(data.getProductId());
    ReceiveSheetDetailExportModel model = new ReceiveSheetDetailExportModel();
    model.setOrderDate(data.getOrderDate());
    model.setSupplierName(data.getSupplierName());
    model.setProductCode(data.getProductCode());
    model.setProductName(data.getProductName());
    model.setShortName(product == null ? null : product.getShortName());
    model.setSpec(data.getSpec());
    model.setUnit(data.getUnit());
    model.setCategoryName(data.getCategoryName());
    model.setTaxPrice(data.getTaxPrice());
    model.setOrderNum(data.getOrderNum());
    model.setTaxAmount(data.getTaxAmount());
    model.setProductionDate(data.getProductionDate());
    model.setDescription(data.getDescription());
    model.setSheetDescription(data.getSheetDescription());
    return model;
  }

  @Override
  public Class<ReceiveSheetDetailExportModel> getModelClass() {
    return ReceiveSheetDetailExportModel.class;
  }
}
