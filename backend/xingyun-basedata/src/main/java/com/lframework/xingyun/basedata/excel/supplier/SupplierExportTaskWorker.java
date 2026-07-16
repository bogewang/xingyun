package com.lframework.xingyun.basedata.excel.supplier;

import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.xingyun.basedata.entity.Supplier;
import com.lframework.xingyun.basedata.service.supplier.SupplierService;
import com.lframework.xingyun.basedata.vo.supplier.QuerySupplierVo;

public class SupplierExportTaskWorker implements
    ExportTaskWorker<QuerySupplierVo, Supplier, SupplierExportModel> {

  @Override
  public QuerySupplierVo parseParams(String json) {
    return JsonUtil.parseObject(json, QuerySupplierVo.class);
  }

  @Override
  public PageResult<Supplier> getDataList(int pageIndex, int pageSize, QuerySupplierVo params) {
    SupplierService supplierService = ApplicationUtil.getBean(SupplierService.class);

    return supplierService.query(pageIndex, pageSize, params);
  }

  @Override
  public SupplierExportModel exportData(Supplier data) {
    return new SupplierExportModel(data);
  }

  @Override
  public Class<SupplierExportModel> getModelClass() {
    return SupplierExportModel.class;
  }
}
