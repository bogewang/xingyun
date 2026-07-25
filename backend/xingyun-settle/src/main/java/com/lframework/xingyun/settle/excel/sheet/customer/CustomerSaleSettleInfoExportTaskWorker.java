package com.lframework.xingyun.settle.excel.sheet.customer;

import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.xingyun.settle.bo.sheet.customer.CustomerSaleSettleInfoBo;
import com.lframework.xingyun.settle.service.CustomerSettleSheetService;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSaleSettleInfoVo;

/**
 * 客户销售结算工作台导出任务。
 */
public class CustomerSaleSettleInfoExportTaskWorker implements ExportTaskWorker<
    QueryCustomerSaleSettleInfoVo, CustomerSaleSettleInfoBo, CustomerSaleSettleInfoExportModel> {

  @Override
  public QueryCustomerSaleSettleInfoVo parseParams(String json) {
    return JsonUtil.parseObject(json, QueryCustomerSaleSettleInfoVo.class);
  }

  @Override
  public PageResult<CustomerSaleSettleInfoBo> getDataList(int pageIndex, int pageSize,
      QueryCustomerSaleSettleInfoVo params) {
    params.setPageIndex(pageIndex);
    params.setPageSize(pageSize);
    CustomerSettleSheetService customerSettleSheetService = ApplicationUtil.getBean(
        CustomerSettleSheetService.class);
    return customerSettleSheetService.querySaleSettleInfos(params);
  }

  @Override
  public CustomerSaleSettleInfoExportModel exportData(CustomerSaleSettleInfoBo data) {
    return new CustomerSaleSettleInfoExportModel(data);
  }

  @Override
  public Class<CustomerSaleSettleInfoExportModel> getModelClass() {
    return CustomerSaleSettleInfoExportModel.class;
  }
}
